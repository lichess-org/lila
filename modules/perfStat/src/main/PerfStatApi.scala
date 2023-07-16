package lila.perfStat

import lila.rating.{ Perf, PerfType, UserRankMap }
import lila.security.Granter
import lila.user.{ LightUserApi, RankingApi, RankingsOf, User, UserApi, Me }

case class PerfStatData(
    user: User.WithPerfs,
    stat: PerfStat,
    ranks: UserRankMap,
    percentile: Option[Double]
):
  def rank = ranks get stat.perfType

final class PerfStatApi(
    storage: PerfStatStorage,
    indexer: PerfStatIndexer,
    userApi: UserApi,
    rankingsOf: RankingsOf,
    rankingApi: RankingApi,
    lightUserApi: LightUserApi
)(using Executor):

  def data(name: UserStr, perfKey: Perf.Key)(using me: Option[Me]): Fu[Option[PerfStatData]] =
    PerfType(perfKey).so: perfType =>
      userApi withPerfs name.id flatMap {
        _.filter: u =>
          (u.enabled.yes && (!u.lame || me.exists(_ is u))) || me.soUse(Granter(_.UserModView))
        .filter: u =>
          !u.isBot || (perfType =!= PerfType.UltraBullet)
        .soFu: u =>
          for
            oldPerfStat <- get(u.user, perfType)
            perfStat = oldPerfStat.copy(playStreak = oldPerfStat.playStreak.checkCurrent)
            distribution <- u.perfs(perfType).established soFu rankingApi.weeklyRatingDistribution(perfType)
            percentile = distribution.map: distrib =>
              val (under, sum) = lila.user.Stat.percentile(distrib, u.perfs(perfType).intRating)
              Math.round(under * 1000.0 / sum) / 10.0
            _ = lightUserApi preloadUser u.user
            _ <- lightUserApi preloadMany perfStat.userIds
          yield PerfStatData(u, perfStat, rankingsOf(u.id), percentile)
      }

  def get(user: User, perfType: PerfType): Fu[PerfStat] =
    storage.find(user.id, perfType) getOrElse indexer.userPerf(user, perfType)
