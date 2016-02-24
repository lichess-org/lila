package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import play.api.libs.json._
import reactivemongo.bson._

import lila.common.LightUser
import lila.db.api.{ $count, $primitive, $gt }
import lila.db.BSON._
import lila.db.Implicits._
import lila.memo.{ ExpireSetMemo, MongoCache }
import lila.rating.{ Perf, PerfType }
import tube.userTube
import User.{ LightPerf, LightCount }

final class Cached(
    nbTtl: FiniteDuration,
    onlineUserIdMemo: ExpireSetMemo,
    mongoCache: MongoCache.Builder,
    rankingApi: RankingApi) {

  private def oneWeekAgo = DateTime.now minusWeeks 1
  private def oneMonthAgo = DateTime.now minusMonths 1

  private val countCache = mongoCache.single[Int](
    prefix = "user:nb",
    f = $count(UserRepo.enabledSelect),
    timeToLive = nbTtl)

  def countEnabled: Fu[Int] = countCache(true)

  private implicit val LightUserBSONHandler = reactivemongo.bson.Macros.handler[LightUser]
  private implicit val LightPerfBSONHandler = reactivemongo.bson.Macros.handler[LightPerf]
  private implicit val LightCountBSONHandler = reactivemongo.bson.Macros.handler[LightCount]

  def leaderboards: Fu[Perfs.Leaderboards] = for {
    bullet ← top10Perf(PerfType.Bullet.key)
    blitz ← top10Perf(PerfType.Blitz.key)
    classical ← top10Perf(PerfType.Classical.key)
    chess960 ← top10Perf(PerfType.Chess960.key)
    kingOfTheHill ← top10Perf(PerfType.KingOfTheHill.key)
    threeCheck ← top10Perf(PerfType.ThreeCheck.key)
    antichess <- top10Perf(PerfType.Antichess.key)
    atomic <- top10Perf(PerfType.Atomic.key)
    horde <- top10Perf(PerfType.Horde.key)
    racingKings <- top10Perf(PerfType.RacingKings.key)
    crazyhouse <- top10Perf(PerfType.Crazyhouse.key)
  } yield Perfs.Leaderboards(
    bullet = bullet,
    blitz = blitz,
    classical = classical,
    crazyhouse = crazyhouse,
    chess960 = chess960,
    kingOfTheHill = kingOfTheHill,
    threeCheck = threeCheck,
    antichess = antichess,
    atomic = atomic,
    horde = horde,
    racingKings = racingKings)

  val top10Perf = mongoCache[Perf.Key, List[LightPerf]](
    prefix = "user:top10:perf",
    f = (perf: Perf.Key) => UserRepo.topPerfSince(perf, oneWeekAgo, 10) map {
      _ flatMap (_ lightPerf perf)
    },
    timeToLive = 10 minutes)

  val top200Perf = mongoCache[Perf.Key, List[User.LightPerf]](
    prefix = "user:top200:perf",
    f = (perf: Perf.Key) => UserRepo.topPerfSince(perf, oneWeekAgo, 200) map {
      _ flatMap (_ lightPerf perf)
    },
    timeToLive = 10 minutes)

  private val topTodayCache = mongoCache.single[List[User.LightPerf]](
    prefix = "user:top:today",
    f = PerfType.leaderboardable.map { perf =>
      UserRepo.topPerfSince(perf.key, DateTime.now minusHours 12, 1).map {
        _.headOption flatMap (_ lightPerf perf.key)
      }
    }.sequenceFu.map(_.flatten),
    timeToLive = 9 minutes)

  def topToday = topTodayCache.apply _

  val topNbGame = mongoCache[Int, List[User.LightCount]](
    prefix = "user:top:nbGame",
    f = nb => UserRepo topNbGame nb map { _ map (_.lightCount) },
    timeToLive = 34 minutes)

  val topOnline = lila.memo.AsyncCache[Int, List[User]](
    f = UserRepo.byIdsSortRating(onlineUserIdMemo.keys, _),
    timeToLive = 10 seconds)

  object ranking {

    def getAll(userId: User.ID): Fu[Map[Perf.Key, Int]] =
      rankingApi.weeklyStableRanking of userId
  }

  object ratingDistribution {

    def apply(perf: PerfType) = rankingApi.weeklyRatingDistribution(perf)
  }
}
