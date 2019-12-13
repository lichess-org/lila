package lila.user

import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.common.{ AtMost, Every, LightUser }
import lila.memo.PeriodicRefreshCache
import lila.rating.{ Perf, PerfType }
import User.{ LightCount, LightPerf }

final class Cached(
    userRepo: UserRepo,
    onlineUserIds: () => Set[User.ID],
    mongoCache: lila.memo.MongoCache.Builder,
    asyncCache: lila.memo.AsyncCache.Builder,
    rankingApi: RankingApi
)(implicit system: akka.actor.ActorSystem) {

  implicit private val LightUserBSONHandler  = Macros.handler[LightUser]
  implicit private val LightPerfBSONHandler  = Macros.handler[LightPerf]
  implicit private val LightCountBSONHandler = Macros.handler[LightCount]

  val top10 = new PeriodicRefreshCache[Perfs.Leaderboards](
    Every(1 minute),
    AtMost(1 minute),
    f = () => rankingApi fetchLeaderboard 10,
    default = Perfs.emptyLeaderboards,
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
    f = PerfType.leaderboardable
      .map { perf =>
        rankingApi.topPerf(perf.id, 1)
      }
      .sequenceFu
      .map(_.flatten),
    timeToLive = 9 minutes
  )

  def topWeek = topWeekCache.apply _

  val topNbGame = mongoCache[Int, List[User.LightCount]](
    prefix = "user:top:nbGame",
    f = nb => userRepo topNbGame nb map { _ map (_.lightCount) },
    timeToLive = 74 minutes,
    keyToString = _.toString
  )

  private val top50OnlineCache = new lila.memo.PeriodicRefreshCache[List[User]](
    every = Every(30 seconds),
    atMost = AtMost(30 seconds),
    f = () => userRepo.byIdsSortRatingNoBot(onlineUserIds(), 50),
    default = Nil,
    initialDelay = 15 seconds
  )
  def getTop50Online = top50OnlineCache.get

  def rankingsOf(userId: User.ID): Map[PerfType, Int] = rankingApi.weeklyStableRanking of userId

  object ratingDistribution {
    def apply(perf: PerfType) = rankingApi.weeklyRatingDistribution(perf)
  }

  private[user] val botIds = asyncCache.single[Set[User.ID]](
    name = "user.botIds",
    f = userRepo.botIds,
    expireAfter = _.ExpireAfterWrite(10 minutes)
  )
}
