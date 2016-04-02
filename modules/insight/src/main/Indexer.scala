package lila.insight

import akka.actor.ActorRef
import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._

import lila.db.dsl._
import lila.db.dsl._
import lila.game.BSONHandlers.gameBSONHandler
import lila.game.{ Game, GameRepo, Query }
import lila.hub.Sequencer
import lila.rating.PerfType
import lila.user.User

private final class Indexer(storage: Storage, sequencer: ActorRef) {

  private implicit val timeout = makeTimeout minutes 5

  def all(user: User): Funit = {
    val p = scala.concurrent.Promise[Unit]()
    sequencer ! Sequencer.work(compute(user), p.some)
    p.future
  }

  def update(game: Game, userId: String, previous: Entry): Funit =
    PovToEntry(game, userId, previous.provisional) flatMap {
      case Right(e) => storage update e.copy(number = previous.number)
      case _        => funit
    }

  private def compute(user: User): Funit = storage.fetchLast(user.id) flatMap {
    case None    => fromScratch(user)
    case Some(e) => computeFrom(user, e.date plusSeconds 1, e.number + 1)
  }

  private def fromScratch(user: User): Funit =
    fetchFirstGame(user) flatMap {
      _.?? { g => computeFrom(user, g.createdAt, 1) }
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
      (user.count.rated >= maxGames) ?? GameRepo.coll
        .find(gameQuery(user))
        .sort(Query.sortCreated)
        .skip(maxGames - 1)
        .one[Game]
    } orElse GameRepo.coll
      .find(gameQuery(user))
      .sort(Query.sortCreated)
      .one[Game]

  private def computeFrom(user: User, from: DateTime, fromNumber: Int): Funit = {
    storage nbByPerf user.id flatMap { nbs =>
      var nbByPerf = nbs
      def toEntry(game: Game): Fu[Option[Entry]] = game.perfType ?? { pt =>
        val nb = nbByPerf.getOrElse(pt, 0) + 1
        nbByPerf = nbByPerf.updated(pt, nb)
        PovToEntry(game, user.id, provisional = nb < 10).addFailureEffect { e =>
          println(e)
          e.printStackTrace
        } map (_.toOption)
      }
      val query = gameQuery(user) ++ $doc(Game.BSONFields.createdAt $gte from)
      GameRepo.sortedCursor(query, Query.sortChronological)
        .enumerate(maxGames, stopOnError = true) &>
        Enumeratee.grouped(Iteratee takeUpTo 4) &>
        Enumeratee.mapM[Seq[Game]].apply[Seq[Entry]] { games =>
          games.map(toEntry).sequenceFu.map(_.flatten).addFailureEffect { e =>
            println(e)
            e.printStackTrace
          }
        } &>
        Enumeratee.grouped(Iteratee takeUpTo 50) |>>>
        Iteratee.foldM[Seq[Seq[Entry]], Int](fromNumber) {
          case (number, xs) =>
            val entries = xs.flatten.sortBy(_.date).zipWithIndex.map {
              case (e, i) => e.copy(number = number + i)
            }
            val nextNumber = number + entries.size
            storage bulkInsert entries inject nextNumber
        }
    } void
  }
}
