package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import reactivemongo.bson._

import lila.common.LightUser
import lila.db.api.{ $count, $primitive }
import lila.db.BSON._
import lila.db.Implicits._
import lila.memo.{ ExpireSetMemo, MongoCache }
import lila.rating.{ Perf, PerfType }
import User.LightPerf
import tube.userTube

final class Cached(
    nbTtl: FiniteDuration,
    onlineUserIdMemo: ExpireSetMemo,
    mongoCache: MongoCache.Builder) {

  private def oneWeekAgo = DateTime.now minusWeeks 1
  private def oneMonthAgo = DateTime.now minusMonths 1

  private val countCache = mongoCache.single[Int](
    prefix = "user:nb",
    f = $count(UserRepo.enabledSelect),
    timeToLive = nbTtl)

  def countEnabled: Fu[Int] = countCache(true)

  private implicit val userHandler = User.userBSONHandler
  private implicit val LightUserBSONHandler = reactivemongo.bson.Macros.handler[LightUser]
  private implicit val LightPerfBSONHandler = reactivemongo.bson.Macros.handler[LightPerf]

  val top10Perf = mongoCache[Perf.Key, List[User]](
    prefix = "user:top10:perf",
    f = (perf: Perf.Key) => UserRepo.topPerfSince(perf, oneWeekAgo, 10),
    timeToLive = 10 minutes)

  val top200Perf = mongoCache[Perf.Key, List[User.LightPerf]](
    prefix = "user:top200:perf",
    f = (perf: Perf.Key) => UserRepo.topPerfSince(perf, oneWeekAgo, 200) map {
      _ flatMap (_ lightPerf perf)
    },
    timeToLive = 10 minutes)

  private case class UserPerf(user: User, perfKey: String)
  private implicit val UserPerfBSONHandler = reactivemongo.bson.Macros.handler[UserPerf]

  private val topTodayCache = mongoCache.single[List[UserPerf]](
    prefix = "user:top:today",
    f = PerfType.leaderboardable.map { perf =>
      UserRepo.topPerfSince(perf.key, DateTime.now minusHours 12, 1) map2 { (u: User) =>
        UserPerf(u, perf.key)
      }
    }.sequenceFu map (_.flatten),
    timeToLive = 9 minutes)

  def topToday(x: Boolean): Fu[List[(User, PerfType)]] =
    topTodayCache(x) map2 { (up: UserPerf) =>
      (up.user, PerfType(up.perfKey) err s"No such perf ${up.perfKey}")
    }

  val topNbGame = mongoCache[Int, List[User]](
    prefix = "user:top:nbGame",
    f = UserRepo.topNbGame,
    timeToLive = 34 minutes)

  val topOnline = lila.memo.AsyncCache[Int, List[User]](
    f = UserRepo.byIdsSortRating(onlineUserIdMemo.keys, _),
    timeToLive = 10 seconds)

  object ranking {

    def getAll(id: User.ID): Fu[Map[Perf.Key, Int]] =
      PerfType.leaderboardable.map { perf =>
        cache(perf.key) map { _ get id map (perf.key -> _) }
      }.sequenceFu map (_.flatten.toMap)

    import lila.db.BSON.MapValue.MapHandler

    private val cache = mongoCache[Perf.Key, Map[User.ID, Int]](
      prefix = "user:ranking",
      f = compute,
      timeToLive = 15 minutes)

    private def compute(perf: Perf.Key): Fu[Map[User.ID, Int]] =
      $primitive(
        UserRepo.topPerfSinceSelect(perf, oneWeekAgo),
        "_id",
        _ sort UserRepo.sortPerfDesc(perf)
      )(_.asOpt[User.ID]) map { _.zipWithIndex.map(x => x._1 -> (x._2 + 1)).toMap }
  }

  object ratingDistribution {

    import lila.db.BSON.MapValue.MapHandler

    private type NbUsers = Int

    def apply(perf: Perf.Key) = cache(perf)

    private val cache = mongoCache[Perf.Key, List[NbUsers]](
      prefix = "user:rating:distribution",
      f = compute,
      timeToLive = 3 hour)

    private def compute(perf: Perf.Key): Fu[List[NbUsers]] =
      UserRepo.ratingDistribution(perf, since = oneMonthAgo)
  }
}
