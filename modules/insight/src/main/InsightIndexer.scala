package lila.insight

import akka.stream.scaladsl._
import org.joda.time.DateTime
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.common.LilaStream
import lila.db.dsl._
import lila.game.BSONHandlers.gameBSONHandler
import lila.game.{ Game, GameRepo, Query }
import lila.user.{ User, UserRepo }

final private class InsightIndexer(
    povToEntry: PovToEntry,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    storage: Storage
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  private val workQueue =
    new lila.hub.DuctSequencer(maxSize = 128, timeout = 2 minutes, name = "insightIndexer")

  def all(userId: User.ID): Funit =
    workQueue {
      userRepo byId userId flatMap {
        _ ?? { user =>
          storage.fetchLast(user.id) flatMap {
            case None    => fromScratch(user)
            case Some(e) => computeFrom(user, e.date plusSeconds 1, e.number + 1)
          }
        }
      }
    }

  def update(game: Game, userId: String, previous: Entry): Funit =
    povToEntry(game, userId, previous.provisional) flatMap {
      case Right(e) => storage update e.copy(number = previous.number)
      case _        => funit
    }

  private def fromScratch(user: User): Funit =
    fetchFirstGame(user) flatMap {
      _.?? { g =>
        computeFrom(user, g.createdAt, 1)
      }
    }

  private def gameQuery(user: User) =
    Query.user(user.id) ++
      Query.rated ++
      Query.finished ++
      Query.turnsGt(2) ++
      Query.notFromPosition ++
      Query.notHordeOrSincePawnsAreWhite

  private val maxGames = 10 * 1000

  private def fetchFirstGame(user: User): Fu[Option[Game]] =
    if (user.count.rated == 0) fuccess(none)
    else {
      (user.count.rated >= maxGames) ?? gameRepo.coll
        .find(gameQuery(user))
        .sort(Query.sortCreated)
        .skip(maxGames - 1)
        .one[Game](readPreference = ReadPreference.secondaryPreferred)
    } orElse gameRepo.coll
      .find(gameQuery(user))
      .sort(Query.sortChronological)
      .one[Game](readPreference = ReadPreference.secondaryPreferred)

  private def computeFrom(user: User, from: DateTime, fromNumber: Int): Funit =
    storage nbByPerf user.id flatMap { nbs =>
      var nbByPerf = nbs
      def toEntry(game: Game): Fu[Option[Entry]] =
        game.perfType ?? { pt =>
          val nb = nbByPerf.getOrElse(pt, 0) + 1
          nbByPerf = nbByPerf.updated(pt, nb)
          povToEntry(game, user.id, provisional = nb < 10).addFailureEffect { e =>
            logger.warn(e.getMessage, e)
          } map (_.toOption)
        }
      val query = gameQuery(user) ++ $doc(Game.BSONFields.createdAt $gte from)
      gameRepo
        .sortedCursor(query, Query.sortChronological)
        .documentSource(maxGames)
        .mapAsync(16)(toEntry)
        .via(LilaStream.collect)
        .zipWithIndex
        .map { case (e, i) => e.copy(number = fromNumber + i.toInt) }
        .grouped(100)
        .map(storage.bulkInsert)
        .toMat(Sink.ignore)(Keep.right)
        .run()
    } void

}
