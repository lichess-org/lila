package lila.game

import lila.db.dsl._
import lila.user.{ User, UserRepo }

import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

final class PlayTimeApi(
    gameColl: Coll,
    asyncCache: lila.memo.AsyncCache.Builder,
    system: akka.actor.ActorSystem
) {

  import Game.{ BSONFields => F }

  def apply(user: User): Fu[Option[User.PlayTime]] = user.playTime match {
    case None => randomlyCompute ?? compute(user)
    case pt => fuccess(pt)
  }

  def randomlyCompute = scala.util.Random.nextInt(5) == 0

  private def compute(user: User): Fu[Option[User.PlayTime]] =
    creationCache.get(user.id).withTimeoutDefault(1 second, none)(system)

  // to avoid creating it twice
  private val creationCache = asyncCache.multi[User.ID, Option[User.PlayTime]](
    name = "playTime",
    f = computeNow,
    resultTimeout = 29.second,
    expireAfter = _.ExpireAfterWrite(30 seconds)
  )

  private def computeNow(userId: User.ID): Fu[Option[User.PlayTime]] =
    UserRepo.getPlayTime(userId) orElse {

      import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._

      def extractSeconds(docs: Iterable[Bdoc], onTv: Boolean): Int = ~docs.collectFirst {
        case doc if doc.getAs[Boolean]("_id").has(onTv) =>
          doc.getAs[Long]("ms") map { millis => (millis / 1000).toInt }
      }.flatten

      gameColl.aggregateList(
        Match($doc(
          F.playerUids -> userId,
          F.clock $exists true
        )),
        List(
          Project($doc(
            F.id -> false,
            "tv" -> $doc("$gt" -> $arr("$tv", BSONNull)),
            "ms" -> $doc("$subtract" -> $arr("$ua", "$ca"))
          )),
          Match($doc("ms" $lt 6.hours.toMillis)),
          GroupField("tv")("ms" -> SumField("ms"))
        ),
        maxDocs = 2,
        ReadPreference.secondaryPreferred
      ).flatMap { docs =>
          val onTvSeconds = extractSeconds(docs, true)
          val offTvSeconds = extractSeconds(docs, false)
          val pt = User.PlayTime(total = onTvSeconds + offTvSeconds, tv = onTvSeconds)
          UserRepo.setPlayTime(userId, pt) inject pt.some
        }
    } recover {
      case e: Exception =>
        logger.warn(s"$userId play time", e)
        none
    }
}
