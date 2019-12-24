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
    mongoCache: lila.memo.MongoCache.Api,
    cacheApi: lila.memo.CacheApi,
    rankingApi: RankingApi
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

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
    PerfType.leaderboardable.size,
    "user:top200:perf",
    19 minutes,
    _.toString
  ) { loader =>
    _.refreshAfterWrite(20 minutes)
      .buildAsyncFuture {
        loader {
          rankingApi.topPerf(_, 200)
        }
      }
  }

  private val topWeekCache = mongoCache.unit[List[User.LightPerf]](
    "user:top:week",
    9 minutes
  ) { loader =>
    _.refreshAfterWrite(10 minutes)
      .buildAsyncFuture {
        loader { _ =>
          PerfType.leaderboardable
            .map { perf =>
              rankingApi.topPerf(perf.id, 1)
            }
            .sequenceFu
            .dmap(_.flatten)
        }
      }
  }

  def topWeek = topWeekCache.get({})

  val top10NbGame = mongoCache.unit[List[User.LightCount]](
    "user:top:nbGame",
    74 minutes
  ) { loader =>
    _.refreshAfterWrite(75 minutes)
      .buildAsyncFuture {
        loader { _ =>
          userRepo topNbGame 10 dmap (_.map(_.lightCount))
        }
      }
  }

  private val top50OnlineCache = new lila.memo.PeriodicRefreshCache[List[User]](
    every = Every(30 seconds),
    atMost = AtMost(30 seconds),
    f = () => userRepo.byIdsSortRatingNoBot(onlineUserIds(), 50),
    default = Nil,
    initialDelay = 15 seconds
  )
  def getTop50Online = top50OnlineCache.get

  def rankingsOf(userId: User.ID): Map[PerfType, Int] = rankingApi.weeklyStableRanking of userId

  private[user] val botIds = cacheApi.unit[Set[User.ID]] {
    _.refreshAfterWrite(10 minutes)
      .buildAsyncFuture(_ => userRepo.botIds)
  }
}
