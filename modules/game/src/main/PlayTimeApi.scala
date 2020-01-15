package lila.game

import lila.db.dsl._
import lila.user.{ User, UserRepo }
import lila.common.WorkQueue

import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

final class PlayTimeApi(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi
)(
    implicit ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer
) {

  import Game.{ BSONFields => F }

  private val workQueue = new WorkQueue(buffer = 512, timeout = 1 minute, name = "playTime", parallelism = 4)

  def apply(user: User): Fu[Option[User.PlayTime]] =
    fuccess(user.playTime) orElse
      creationCache
        .get(user.id)
        .withTimeoutDefault(100 millis, none)
        .nevermind

  // to avoid creating it twice
  private val creationCache = cacheApi[User.ID, Option[User.PlayTime]](64, "playTime") {
    _.expireAfterWrite(5 minutes)
      .buildAsyncFuture { userId =>
        workQueue(computeNow(userId)).monSuccess(_.playTime.create)
      }
  }

  private def computeNow(userId: User.ID): Fu[Option[User.PlayTime]] =
    userRepo.getPlayTime(userId) orElse {

      def extractSeconds(docs: Iterable[Bdoc], onTv: Boolean): Int =
        ~docs.collectFirst {
          case doc if doc.getAsOpt[Boolean]("_id").has(onTv) =>
            doc.long("ms") map { millis =>
              (millis / 1000).toInt
            }
        }.flatten

      gameRepo.coll
        .aggregateList(
          maxDocs = 2,
          ReadPreference.secondaryPreferred
        ) { framework =>
          import framework._
          Match(
            $doc(
              F.playerUids -> userId,
              F.clock $exists true
            )
          ) -> List(
            Project(
              $doc(
                F.id -> false,
                "tv" -> $doc("$gt" -> $arr("$tv", BSONNull)),
                "ms" -> $doc("$subtract" -> $arr("$ua", "$ca"))
              )
            ),
            Match($doc("ms" $lt 6.hours.toMillis)),
            GroupField("tv")("ms" -> SumField("ms"))
          )
        }
        .flatMap { docs =>
          val onTvSeconds  = extractSeconds(docs, true)
          val offTvSeconds = extractSeconds(docs, false)
          val pt           = User.PlayTime(total = onTvSeconds + offTvSeconds, tv = onTvSeconds)
          lila.mon.playTime.createPlayTime.record(pt.total)
          userRepo.setPlayTime(userId, pt) inject pt.some
        }
    } recover {
      case e: Exception =>
        logger.warn(s"$userId play time", e)
        none
    }
}
