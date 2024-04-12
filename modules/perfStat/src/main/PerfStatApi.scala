package lila.perfStat

import lila.rating.{ Perf, UserRankMap }
import lila.core.perm.Granter
import lila.user.{ LightUserApi, Me, RankingApi, RankingsOf, User, UserApi }

import lila.rating.PerfType
import lila.rating.PerfExt.established
import lila.core.perf.UserWithPerfs
import lila.core.perf.PerfKeyStr

case class PerfStatData(
    user: UserWithPerfs,
    stat: PerfStat,
    ranks: UserRankMap,
    percentile: Option[Double],
    percentileLow: Option[Double],
    percentileHigh: Option[Double]
):
  def rank = ranks.get(stat.perfType.key)

final class PerfStatApi(
    storage: PerfStatStorage,
    indexer: PerfStatIndexer,
    userApi: UserApi,
    rankingsOf: RankingsOf,
    rankingApi: RankingApi,
    lightUserApi: LightUserApi
)(using Executor)
    extends lila.core.perf.PerfStatApi:

  def data(name: UserStr, perfKey: PerfKeyStr)(using me: Option[Me]): Fu[Option[PerfStatData]] =
    PerfKey
      .read(perfKey)
      .so: perfKey =>
        userApi.withPerfs(name.id).flatMap {
          _.filter: u =>
            (u.enabled.yes && (!u.lame || me.exists(_.is(u.user)))) || me.soUse(Granter[Me](_.UserModView))
          .filter: u =>
            !u.isBot || (perfKey != PerfKey.ultraBullet)
          .soFu: u =>
            for
              oldPerfStat <- get(u.user.id, perfKey)
              perfStat = oldPerfStat.copy(playStreak = oldPerfStat.playStreak.checkCurrent)
              distribution <- u
                .perfs(perfKey)
                .established
                .soFu(rankingApi.weeklyRatingDistribution(perfKey))
              percentile     = calcPercentile(distribution, u.perfs(perfKey).intRating)
              percentileLow  = perfStat.lowest.flatMap { r => calcPercentile(distribution, r.int) }
              percentileHigh = perfStat.highest.flatMap { r => calcPercentile(distribution, r.int) }
              _              = lightUserApi.preloadUser(u.user)
              _ <- lightUserApi.preloadMany(perfStat.userIds)
            yield PerfStatData(u, perfStat, rankingsOf(u.id), percentile, percentileLow, percentileHigh)
        }

  private def calcPercentile(wrd: Option[List[Int]], intRating: IntRating): Option[Double] =
    wrd.map: distrib =>
      val (under, sum) = lila.user.Stat.percentile(distrib, intRating)
      Math.round(under * 1000.0 / sum) / 10.0

  def get(user: UserId, perfType: PerfType): Fu[PerfStat] =
    storage.find(user, perfType).getOrElse(indexer.userPerf(user, perfType))

  def highestRating(user: UserId, perfKey: PerfKey): Fu[Option[IntRating]] =
    get(user, perfKey).map(_.highest.map(_.int))
