package lila.coach

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
import lila.hub.Sequencer
import lila.user.User

final class Aggregator(storage: Storage, sequencer: ActorRef) {

  private implicit val timeout = makeTimeout.minutes(5)

  def apply(user: User): Funit = {
    val p = scala.concurrent.Promise[Unit]()
    sequencer ! Sequencer.work(compute(user), p.some)
    p.future
  }

  private def compute(user: User): Funit = storage.fetchLast(user.id) flatMap {
    case None    => fromScratch(user)
    case Some(e) => computeFrom(user, e.date plusSeconds 1)
  }

  private def fromScratch(user: User): Funit =
    fetchFirstGame(user) flatMap {
      _.?? { g => computeFrom(user, g.createdAt) }
    }

  private def gameQuery(user: User) = Query.user(user.id) ++ Query.rated ++ Query.finished
  private val maxGames = 5 * 10
  // private val maxGames = 5 * 1000

  private def fetchFirstGame(user: User): Fu[Option[Game]] =
    if (user.count.rated == 0) fuccess(none)
    else {
      (user.count.rated >= maxGames) ??
        pimpQB($query(gameQuery(user))).sort(Query.sortCreated).skip(maxGames - 1).one[Game]
    } orElse
      pimpQB($query(gameQuery(user))).sort(Query.sortChronological).one[Game]

  private def computeFrom(user: User, from: DateTime): Funit =
    lila.common.Chronometer.log(s"coach aggregator:${user.username}") {
      loginfo(s"[coach] start aggregating ${user.username} games")
      pimpQB($query(gameQuery(user) ++ Json.obj(Game.BSONFields.createdAt -> $gte($date(from)))))
        .sort(Query.sortChronological)
        .cursor[Game]()
        .enumerate(maxGames, stopOnError = true) &>
        Enumeratee.mapM[lila.game.Game].apply[Either[Game, Entry]] { game =>
          PovToEntry(game, user.id).addFailureEffect { e =>
            println(e)
            e.printStackTrace
          }
        } |>>>
        Iteratee.foldM[Either[Game, Entry], Int](0) {
          case (nb, Right(e)) =>
            if (nb % 100 == 0) loginfo(s"[coach ${user.username}] aggregated $nb games")
            storage insert e inject (nb + 1)
          case (nb, Left(g)) =>
            logwarn(s"[coach ${user.username}] invalid game http://l.org/${g.id}")
            fuccess(nb)
        } addEffect { nb =>
          loginfo(s"[coach ${user.username}] done aggregating $nb games")
        } void
    }
}
