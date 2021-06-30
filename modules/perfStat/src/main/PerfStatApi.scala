package lila.perfStat

import scala.concurrent.ExecutionContext

import lila.rating.{ PerfType, UserRankMap }
import lila.security.Granter
import lila.user.User
import lila.user.{ LightUserApi, RankingApi, RankingsOf, User, UserRepo }

case class PerfStatData(
    user: User,
    stat: PerfStat,
    ranks: UserRankMap,
    percentile: Option[Double]
) {
  def rank = ranks get stat.perfType
}

final class PerfStatApi(
    storage: PerfStatStorage,
    indexer: PerfStatIndexer,
    userRepo: UserRepo,
    rankingsOf: RankingsOf,
    rankingApi: RankingApi,
    lightUserApi: LightUserApi
)(implicit
    ec: ExecutionContext
) {

  def data(name: String, perfKey: String, by: Option[User]): Fu[Option[PerfStatData]] =
    PerfType(perfKey) ?? { perfType =>
      userRepo named name flatMap {
        _.filter { u =>
          (u.enabled && (!u.lame || by.exists(u.is))) || by.??(Granter(_.UserModView))
        } ?? { u =>
          for {
            ranks       <- rankingsOf(u.id)
            oldPerfStat <- get(u, perfType)
            perfStat = oldPerfStat.copy(playStreak = oldPerfStat.playStreak.checkCurrent)
            distribution <- u.perfs(perfType).established ?? {
              rankingApi.weeklyRatingDistribution(perfType) dmap some
            }
            percentile = distribution.map { distrib =>
              lila.user.Stat.percentile(distrib, u.perfs(perfType).intRating) match {
                case (under, sum) => Math.round(under * 1000.0 / sum) / 10.0
              }
            }
            _ = lightUserApi preloadUser u
            _ <- lightUserApi preloadMany perfStat.userIds.map(_.value)
          } yield PerfStatData(u, perfStat, ranks, percentile).some
        }
      }
    }

  def get(user: lila.user.User, perfType: lila.rating.PerfType): Fu[PerfStat] =
    storage.find(user.id, perfType) getOrElse indexer.userPerf(user, perfType)
}
