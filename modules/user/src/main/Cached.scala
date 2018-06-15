package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.common.LightUser
import lila.db.dsl._
import lila.rating.{ Perf, PerfType }
import User.{ LightPerf, LightCount }

final class Cached(
    userColl: Coll,
    nbTtl: FiniteDuration,
    onlineUserIdMemo: lila.memo.ExpireSetMemo,
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    rankingApi: RankingApi
) {

  private def oneWeekAgo = DateTime.now minusWeeks 1
  private def oneMonthAgo = DateTime.now minusMonths 1

  private implicit val LightUserBSONHandler = Macros.handler[LightUser]
  private implicit val LightPerfBSONHandler = Macros.handler[LightPerf]
  private implicit val LightCountBSONHandler = Macros.handler[LightCount]

  def leaderboards: Fu[Perfs.Leaderboards] = for {
    ultraBullet ← top10Perf(PerfType.UltraBullet.id)
    bullet ← top10Perf(PerfType.Bullet.id)
    blitz ← top10Perf(PerfType.Blitz.id)
    rapid ← top10Perf(PerfType.Rapid.id)
    classical ← top10Perf(PerfType.Classical.id)
    chess960 ← top10Perf(PerfType.Chess960.id)
    kingOfTheHill ← top10Perf(PerfType.KingOfTheHill.id)
    threeCheck ← top10Perf(PerfType.ThreeCheck.id)
    antichess <- top10Perf(PerfType.Antichess.id)
    atomic <- top10Perf(PerfType.Atomic.id)
    horde <- top10Perf(PerfType.Horde.id)
    racingKings <- top10Perf(PerfType.RacingKings.id)
    crazyhouse <- top10Perf(PerfType.Crazyhouse.id)
  } yield Perfs.Leaderboards(
    ultraBullet = ultraBullet,
    bullet = bullet,
    blitz = blitz,
    rapid = rapid,
    classical = classical,
    crazyhouse = crazyhouse,
    chess960 = chess960,
    kingOfTheHill = kingOfTheHill,
    threeCheck = threeCheck,
    antichess = antichess,
    atomic = atomic,
    horde = horde,
    racingKings = racingKings
  )

  val top10Perf = mongoCache[Perf.ID, List[LightPerf]](
    prefix = "user:top10:perf",
    f = (perf: Perf.ID) => rankingApi.topPerf(perf, 10),
    timeToLive = 10 seconds,
    keyToString = _.toString
  )

  val top200Perf = mongoCache[Perf.ID, List[User.LightPerf]](
    prefix = "user:top200:perf",
    f = (perf: Perf.ID) => rankingApi.topPerf(perf, 200),
    timeToLive = 10 minutes,
    keyToString = _.toString
  )

  private val topWeekCache = mongoCache.single[List[User.LightPerf]](
    prefix = "user:top:week",
    f = PerfType.leaderboardable.map { perf =>
      rankingApi.topPerf(perf.id, 1)
    }.sequenceFu.map(_.flatten),
    timeToLive = 9 minutes
  )

  def topWeek = topWeekCache.apply _

  val topNbGame = mongoCache[Int, List[User.LightCount]](
    prefix = "user:top:nbGame",
    f = nb => UserRepo topNbGame nb map { _ map (_.lightCount) },
    timeToLive = 34 minutes,
    keyToString = _.toString
  )

  private val top50Online = asyncCache.single[List[User]](
    name = "user.top50online",
    f = UserRepo.byIdsSortRatingNoBot(onlineUserIdMemo.keys, 50),
    expireAfter = _.ExpireAfterWrite(10 seconds)
  )

  def getTop50Online = top50Online.get.nevermind

  object ranking {

    def getAll(userId: User.ID): Fu[Map[Perf.Key, Int]] =
      rankingApi.weeklyStableRanking of userId
  }

  object ratingDistribution {

    def apply(perf: PerfType) = rankingApi.weeklyRatingDistribution(perf)
  }
}
