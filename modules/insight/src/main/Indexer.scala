package lila.insight

import akka.actor.ActorRef
import org.joda.time.DateTime
import play.api.libs.iteratee._
import play.api.libs.json.Json
import reactivemongo.bson._

import lila.db.api._
import lila.db.BSON._
import lila.db.Implicits._
import lila.game.BSONHandlers.gameBSONHandler
import lila.game.tube.gameTube
import lila.game.{ Game, Query }
import lila.rating.PerfType
import lila.hub.Sequencer
import lila.user.User

private final class Indexer(storage: Storage, sequencer: ActorRef) {

  private implicit val timeout = makeTimeout.minutes(5)

  def all(user: User): Funit = {
    val p = scala.concurrent.Promise[Unit]()
    sequencer ! Sequencer.work(compute(user), p.some)
    p.future
  }

  def one(game: Game, userId: String, provisional: Boolean): Funit =
    PovToEntry(game, userId, provisional) flatMap {
      case Right(e) => storage update e
      case Left(g) =>
        logwarn(s"[insight $userId] invalid game http://l.org/${g.id}")
        funit
    }

  private def compute(user: User): Funit = storage.fetchLast(user.id) flatMap {
    case None    => fromScratch(user)
    case Some(e) => computeFrom(user, e.date plusSeconds 1)
  }

  private def fromScratch(user: User): Funit =
    fetchFirstGame(user) flatMap {
      _.?? { g => computeFrom(user, g.createdAt) }
    }

  private def gameQuery(user: User) = Query.user(user.id) ++
    Query.rated ++
    Query.finished ++
    Query.turnsMoreThan(2) ++
    Query.notFromPosition ++
    Query.notHordeOrSincePawnsAreWhite

  // private val maxGames = 1 * 10
  private val maxGames = 10 * 1000

  private def fetchFirstGame(user: User): Fu[Option[Game]] =
    if (user.count.rated == 0) fuccess(none)
    else {
      (user.count.rated >= maxGames) ??
        pimpQB($query(gameQuery(user))).sort(Query.sortCreated).skip(maxGames - 1).one[Game]
    } orElse
      pimpQB($query(gameQuery(user))).sort(Query.sortChronological).one[Game]

  private def computeFrom(user: User, from: DateTime): Funit =
    lila.common.Chronometer.log(s"insight aggregator:${user.username}") {
      var nbByPerf = collection.immutable.Map.empty[PerfType, Int]
      def toEntry(game: Game): Fu[Option[Entry]] = game.perfType ?? { pt =>
        val nb = nbByPerf.getOrElse(pt, 0) + 1
        nbByPerf = nbByPerf.updated(pt, nb)
        PovToEntry(game, user.id, provisional = nb < 10).addFailureEffect { e =>
          println(e)
          e.printStackTrace
        } map {
          case Right(e) => e.some
          case Left(g) =>
            logwarn(s"[insight ${user.username}] invalid game http://lichess.org/${g.id}")
            none
        }
      }
      loginfo(s"[insight] start aggregating ${user.username} games")
      val query = $query(gameQuery(user) ++ Json.obj(Game.BSONFields.createdAt -> $gte($date(from))))
      // val query = $query(gameQuery(user) ++ Query.analysed(true) ++ Json.obj(Game.BSONFields.createdAt -> $gte($date(from))))
      pimpQB(query)
        .sort(Query.sortChronological)
        .cursor[Game]()
        .enumerate(maxGames, stopOnError = true) &>
        Enumeratee.grouped(Iteratee takeUpTo 4) &>
        Enumeratee.mapM[Seq[Game]].apply[Seq[Entry]] { games =>
          games.map(toEntry).sequenceFu.map(_.flatten).addFailureEffect { e =>
            println(e)
            e.printStackTrace
          }
        } &>
        Enumeratee.grouped(Iteratee takeUpTo 50) |>>>
        Iteratee.foldM[Seq[Seq[Entry]], Int](0) {
          case (nb, xs) =>
            val entries = xs.flatten
            loginfo(s"[insight ${user.username}] $nb")
            storage bulkInsert entries inject (nb + entries.size)
        } addEffect { nb =>
          loginfo(s"[insight ${user.username}] done aggregating $nb games")
        } void
    }
}
