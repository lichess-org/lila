package lidraughts.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import reactivemongo.bson._

import lidraughts.common.LightUser
import lidraughts.db.dsl._
import lidraughts.rating.{ Perf, PerfType }
import User.{ LightPerf, LightCount }

final class Cached(
    userColl: Coll,
    nbTtl: FiniteDuration,
    onlineUserIdMemo: lidraughts.memo.ExpireSetMemo,
    mongoCache: lidraughts.memo.MongoCache.Builder,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
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
    frisian ← top10Perf(PerfType.Frisian.id)
    antidraughts ← top10Perf(PerfType.Antidraughts.id)
  } yield Perfs.Leaderboards(
    ultraBullet = ultraBullet,
    bullet = bullet,
    blitz = blitz,
    rapid = rapid,
    classical = classical,
    frisian = frisian,
    antidraughts = antidraughts
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
    f = UserRepo.byIdsSortRating(onlineUserIdMemo.keys, 50),
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
