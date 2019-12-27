package lila.game

import akka.stream.scaladsl._
import akka.stream.{ Materializer, OverflowStrategy, QueueOfferResult }
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Promise }
import scala.util.chaining._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

final class CrosstableApi(
    coll: Coll,
    matchupColl: Coll,
    gameRepo: GameRepo,
    userRepo: UserRepo
)(
    implicit ec: ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: Materializer
) {

  import Crosstable.{ Matchup, Result }
  import Crosstable.{ BSONFields => F }
  import Game.{ BSONFields => GF }

  def apply(game: Game): Fu[Option[Crosstable]] = game.twoUserIds ?? {
    case (u1, u2) => apply(u1, u2) dmap some
  }

  def withMatchup(game: Game): Fu[Option[Crosstable.WithMatchup]] = game.twoUserIds ?? {
    case (u1, u2) => withMatchup(u1, u2) dmap some
  }

  def apply(u1: User.ID, u2: User.ID, timeout: FiniteDuration = 1.second): Fu[Crosstable] =
    justFetch(u1, u2) getOrElse createWithTimeout(u1, u2, timeout)

  def withMatchup(u1: User.ID, u2: User.ID, timeout: FiniteDuration = 1.second): Fu[Crosstable.WithMatchup] =
    apply(u1, u2, timeout) zip getMatchup(u1, u2) dmap {
      case crosstable ~ matchup => Crosstable.WithMatchup(crosstable, matchup)
    }

  def justFetch(u1: User.ID, u2: User.ID): Fu[Option[Crosstable]] =
    coll.one[Crosstable](select(u1, u2))

  def fetchOrEmpty(u1: User.ID, u2: User.ID): Fu[Crosstable] =
    justFetch(u1, u2) dmap { _ | Crosstable.empty(u1, u2) }

  def nbGames(u1: User.ID, u2: User.ID): Fu[Int] =
    coll
      .find(
        select(u1, u2),
        $doc("s1" -> true, "s2" -> true).some
      )
      .one[Bdoc] dmap { res =>
      ~(for {
        o  <- res
        s1 <- o.int("s1")
        s2 <- o.int("s2")
      } yield (s1 + s2) / 10)
    }

  def add(game: Game): Funit = game.userIds.distinct.sorted match {
    case List(u1, u2) =>
      val result     = Result(game.id, game.winnerUserId)
      val bsonResult = Crosstable.crosstableBSONHandler.writeResult(result, u1)
      def incScore(userId: User.ID): Int = game.winnerUserId match {
        case Some(u) if u == userId => 10
        case None                   => 5
        case _                      => 0
      }
      val inc1 = incScore(u1)
      val inc2 = incScore(u2)
      val updateCrosstable = coll.update.one(
        select(u1, u2),
        $inc(
          F.score1 -> inc1,
          F.score2 -> inc2
        ) ++ $push(
          Crosstable.BSONFields.results -> $doc(
            "$each"  -> List(bsonResult),
            "$slice" -> -Crosstable.maxGames
          )
        )
      )
      val updateMatchup =
        matchupColl.update.one(
          select(u1, u2),
          $inc(
            F.score1 -> inc1,
            F.score2 -> inc2
          ) ++ $set(
            F.lastPlayed -> DateTime.now
          ),
          upsert = true
        )
      updateCrosstable zip updateMatchup void
    case _ => funit
  }

  private val matchupProjection = $doc(F.lastPlayed -> false)

  private def getMatchup(u1: User.ID, u2: User.ID): Fu[Option[Matchup]] =
    matchupColl.find(select(u1, u2), matchupProjection.some).one[Matchup]

  private def createWithTimeout(u1: User.ID, u2: User.ID, timeout: FiniteDuration): Fu[Crosstable] =
    creationCache
      .get(u1 -> u2)
      .withTimeoutDefault(timeout, Crosstable.empty(u1, u2))

  type UserPair = (User.ID, User.ID)
  type Creation = (UserPair, Promise[Crosstable])

  private val creationQueue = Source
    .queue[Creation](512, OverflowStrategy.dropNew)
    .mapAsyncUnordered(12) {
      case ((u1, u2), promise) =>
        justFetch(u1, u2) flatMap {
          case Some(found) =>
            lila.mon.crosstable.found.increment()
            fuccess(found)
          case _ =>
            create(u1, u2) recoverWith lila.db.recoverDuplicateKey { _ =>
              lila.mon.crosstable.duplicate.increment()
              fetchOrEmpty(u1, u2)
            } recover {
              case e: Exception =>
                logger.error("CrosstableApi.create", e)
                Crosstable.empty(u1, u2)
            }
        } tap promise.completeWith
    }
    .toMat(Sink.ignore)(Keep.left)
    .run

  private val creationCache = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(5 minutes)
    .buildAsyncFuture[UserPair, Crosstable] { users =>
      val promise = Promise[Crosstable]
      creationQueue.offer(users -> promise) flatMap {
        case QueueOfferResult.Enqueued =>
          lila.mon.crosstable.createOffer("success").increment()
          promise.future.monSuccess(_.crosstable.create)
        case result =>
          lila.mon.crosstable.createOffer(result.toString).increment()
          fuccess(Crosstable.empty(users._1, users._2))
      }
    }

  private def create(x1: User.ID, x2: User.ID): Fu[Crosstable] =
    userRepo.orderByGameCount(x1, x2) dmap (_ -> List(x1, x2).sorted) flatMap {
      case (Some((u1, u2)), List(su1, su2)) =>
        val selector = $doc(
          GF.playerUids $all List(u1, u2),
          GF.status $gte chess.Status.Mate.id
        )

        import reactivemongo.api.ReadPreference

        gameRepo.coll
          .find(selector, $doc(GF.winnerId -> true).some)
          .sort($doc(GF.createdAt -> -1))
          .cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred)
          .gather[List]()
          .map { docs =>
            val (s1, s2) = docs.foldLeft(0 -> 0) {
              case ((s1, s2), doc) =>
                doc.getAsOpt[User.ID](GF.winnerId) match {
                  case Some(u) if u == su1 => (s1 + 10, s2)
                  case Some(u) if u == su2 => (s1, s2 + 10)
                  case _                   => (s1 + 5, s2 + 5)
                }
            }
            Crosstable(
              Crosstable.Users(
                Crosstable.User(su1, s1),
                Crosstable.User(su2, s2)
              ),
              results = docs
                .take(Crosstable.maxGames)
                .flatMap { doc =>
                  doc.string(GF.id).map { id =>
                    Result(id, doc.getAsOpt[User.ID](GF.winnerId))
                  }
                }
                .reverse
            )
          } flatMap { crosstable =>
          lila.mon.crosstable.createNbGames.record(crosstable.nbGames)
          coll.insert.one(crosstable) inject crosstable
        }

      case _ => fuccess(Crosstable.empty(x1, x2))
    }

  private def select(u1: User.ID, u2: User.ID) =
    $id(Crosstable.makeKey(u1, u2))
}
