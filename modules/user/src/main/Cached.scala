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
import tube.userTube

final class Cached(
    nbTtl: FiniteDuration,
    onlineUserIdMemo: ExpireSetMemo,
    mongoCache: MongoCache.Builder) {

  private def twoWeeksAgo = DateTime.now minusWeeks 2

  private val countCache = mongoCache.single[Int](
    prefix = "user:nb",
    f = $count(UserRepo.enabledSelect),
    timeToLive = nbTtl)

  def countEnabled: Fu[Int] = countCache(true)

  val leaderboardSize = 10

  private implicit val userHandler = User.userBSONHandler

  val topPerf = mongoCache[Perf.Key, List[User]](
    prefix = "user:top:perf",
    f = (perf: Perf.Key) => UserRepo.topPerfSince(perf, twoWeeksAgo, leaderboardSize),
    timeToLive = 15 minutes)

  private case class UserPerf(user: User, perfKey: String)
  private implicit val UserPerfBSONHandler = reactivemongo.bson.Macros.handler[UserPerf]

  private val topTodayCache = mongoCache.single[List[UserPerf]](
    prefix = "user:top:today",
    f = PerfType.leaderboardable.map { perf =>
      UserRepo.topPerfSince(perf.key, DateTime.now minusHours 12, 1) map2 { (u: User) =>
        UserPerf(u, perf.key)
      }
    }.sequenceFu map (_.flatten),
    timeToLive = 14 minutes)

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

  val topToints = mongoCache[Int, List[User]](
    prefix = "user:toint:online",
    f = UserRepo.allSortToints,
    timeToLive = 21 minutes)

  object ranking {

    def getAll(id: User.ID): Fu[Map[Perf.Key, Int]] =
      PerfType.leaderboardable.map { perf =>
        cache(perf.key) map { _ get id map (perf.key -> _) }
      }.sequenceFu map (_.flatten.toMap)

    import lila.db.BSON.MapValue.MapHandler

    private val cache = mongoCache[Perf.Key, Map[User.ID, Int]](
      prefix = "user:ranking",
      f = compute,
      timeToLive = 33 minutes)

    private def compute(perf: Perf.Key): Fu[Map[User.ID, Int]] =
      $primitive(
        UserRepo.topPerfSinceSelect(perf, twoWeeksAgo),
        "_id",
        _ sort UserRepo.sortPerfDesc(perf)
      )(_.asOpt[User.ID]) map { _.zipWithIndex.map(x => x._1 -> (x._2 + 1)).toMap }
  }
}
