package lila.user

import scala.concurrent.duration._

import org.joda.time.DateTime
import reactivemongo.bson._

import lila.common.{ LightUser, Every, AtMost }
import lila.db.dsl._
import lila.memo.PeriodicRefreshCache
import lila.rating.{ Perf, PerfType }
import User.{ LightPerf, LightCount }

final class Cached(
    userColl: Coll,
    nbTtl: FiniteDuration,
    onlineUserIds: () => Set[User.ID],
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    rankingApi: RankingApi
)(implicit system: akka.actor.ActorSystem) {

  private implicit val LightUserBSONHandler = Macros.handler[LightUser]
  private implicit val LightPerfBSONHandler = Macros.handler[LightPerf]
  private implicit val LightCountBSONHandler = Macros.handler[LightCount]

  val top10 = new PeriodicRefreshCache[Perfs.Leaderboards](
    Every(1 minute),
    AtMost(1 minute),
    f = () => rankingApi fetchLeaderboard 10,
    default = Perfs.emptyLeaderboards,
    logger = logger,
    initialDelay = 30 seconds
  )

  val top200Perf = mongoCache[Perf.ID, List[User.LightPerf]](
    prefix = "user:top200:perf",
    f = (perf: Perf.ID) => rankingApi.topPerf(perf, 200),
    timeToLive = 16 minutes,
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
    timeToLive = 74 minutes,
    keyToString = _.toString
  )

  private val top50OnlineCache = new lila.memo.PeriodicRefreshCache[List[User]](
    every = Every(30 seconds),
    atMost = AtMost(30 seconds),
    f = () => UserRepo.byIdsSortRatingNoBot(onlineUserIds(), 50),
    default = Nil,
    logger = logger branch "top50online",
    initialDelay = 15 seconds
  )
  def getTop50Online = top50OnlineCache.get

  def rankingsOf(userId: User.ID): Map[PerfType, Int] = rankingApi.weeklyStableRanking of userId

  object ratingDistribution {
    def apply(perf: PerfType) = rankingApi.weeklyRatingDistribution(perf)
  }

  val botIds = asyncCache.single[Set[User.ID]](
    name = "user.botIds",
    f = UserRepo.botIds,
    expireAfter = _.ExpireAfterWrite(10 minutes)
  )
}
