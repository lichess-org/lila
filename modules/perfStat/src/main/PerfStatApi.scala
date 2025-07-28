package lila.perfStat

import chess.IntRating
import lila.core.perf.{ PerfId, UserWithPerfs }
import lila.core.perm.Granter
import lila.rating.Glicko.minRating
import lila.rating.PerfExt.established
import lila.rating.{ PerfType, UserRankMap }
import lila.rating.PerfType.GamePerf

case class PerfStatData(
    user: UserWithPerfs,
    stat: PerfStat,
    ranks: UserRankMap,
    percentile: Option[Double],
    percentileLow: Option[Double],
    percentileHigh: Option[Double]
):
  export stat.perfKey
  def rank = ranks.get(stat.perfType.key)

final class PerfStatApi(
    storage: PerfStatStorage,
    indexer: PerfStatIndexer,
    mongoCache: lila.memo.MongoCache.Api,
    userApi: lila.core.user.UserApi,
    rankingRepo: lila.core.user.RankingRepo,
    rankingsOf: UserId => UserRankMap,
    lightUserApi: lila.core.user.LightUserApi
)(using Executor)
    extends lila.core.perf.PerfStatApi:

  def data(name: UserStr, perfKey: PerfKey, computeIfNeeded: Boolean)(using
      me: Option[Me]
  ): Fu[Option[PerfStatData]] =
    PerfType(perfKey) match
      case pk: GamePerf =>
        userApi
          .withPerfs(name.id)
          .flatMap:
            _.filter: u =>
              (u.enabled.yes && (!u.lame || me.exists(_.is(u.user)))) || me.soUse(Granter(_.UserModView))
            .filter: u =>
              !u.isBot || (perfKey != PerfKey.ultraBullet)
            .soFu: u =>
              for
                oldPerfStat <- get(u.user.id, pk, computeIfNeeded)
                perfStat = oldPerfStat.copy(playStreak = oldPerfStat.playStreak.checkCurrent)
                distribution <- u
                  .perfs(perfKey)
                  .established
                  .soFu(weeklyRatingDistribution(perfKey))
                percentile = calcPercentile(distribution, u.perfs(perfKey).intRating)
                percentileLow = perfStat.lowest.flatMap { r => calcPercentile(distribution, r.int) }
                percentileHigh = perfStat.highest.flatMap { r => calcPercentile(distribution, r.int) }
                _ = lightUserApi.preloadUser(u.user)
                _ <- lightUserApi.preloadMany(perfStat.userIds)
              yield PerfStatData(u, perfStat, rankingsOf(u.id), percentile, percentileLow, percentileHigh)
      case _ => fuccess(none)

  private def calcPercentile(wrd: Option[List[Int]], intRating: IntRating): Option[Double] =
    wrd.map: distrib =>
      val (under, sum) = percentileOf(distrib, intRating)
      Math.round(under * 1000.0 / sum) / 10.0

  def get(user: UserId, perf: GamePerf, computeIfNeeded: Boolean): Fu[PerfStat] =
    storage
      .find(user, perf)
      .getOrElse:
        if computeIfNeeded then indexer.userPerf(user, perf)
        else fuccess(PerfStat.init(user, perf))

  def highestRating(user: UserId, perfKey: PerfKey): Fu[Option[IntRating]] =
    PerfType
      .gamePerf(perfKey)
      .so: (gp: GamePerf) =>
        get(user, gp, computeIfNeeded = true).map(_.highest.map(_.int))

  object weeklyRatingDistribution:

    private type NbUsers = Int

    def apply(pt: PerfType): Fu[List[NbUsers]] = cache.get(pt.id)

    private val cache = mongoCache[PerfId, List[NbUsers]](
      lila.rating.PerfType.leaderboardable.size,
      "user:rating:distribution",
      179.minutes,
      _.toString
    ): loader =>
      _.refreshAfterWrite(180.minutes).buildAsyncFuture:
        loader(compute)

    // from 600 to 2800 by Stat.group
    private def compute(perfId: PerfId): Fu[List[NbUsers]] =
      PerfType(perfId)
        .exists(lila.rating.PerfType.isLeaderboardable(_))
        .so:
          import lila.db.dsl.{ *, given }
          rankingRepo
            .coll[List[NbUsers]]: c =>
              c.aggregateList(maxDocs = Int.MaxValue): framework =>
                import framework.*
                Match($doc("perf" -> perfId)) -> List(
                  Project(
                    $doc(
                      "_id" -> false,
                      "r" -> $doc(
                        "$subtract" -> $arr(
                          "$rating",
                          $doc("$mod" -> $arr("$rating", percentileOf.group))
                        )
                      )
                    )
                  ),
                  GroupField("r")("nb" -> SumAll)
                )
              .map: res =>
                  val hash: Map[Int, NbUsers] = res.view
                    .flatMap: obj =>
                      for
                        rating <- obj.int("_id")
                        nb <- obj.getAsOpt[NbUsers]("nb")
                      yield rating -> nb
                    .to(Map)
                  (minRating.value to 2800 by percentileOf.group)
                    .map(hash.getOrElse(_, 0))
                    .toList
            .addEffect(monitorRatingDistribution(perfId))

    /* monitors cumulated ratio of players in each rating group, for a perf
     *
     * rating.distribution.bullet.600 => 0.0003
     * rating.distribution.bullet.800 => 0.0012
     * rating.distribution.bullet.825 => 0.0057
     * rating.distribution.bullet.850 => 0.0102
     * ...
     * rating.distribution.bullet.1500 => 0.5 (hopefully)
     * ...
     * rating.distribution.bullet.2800 => 0.9997
     */
    private def monitorRatingDistribution(perfId: PerfId)(nbUsersList: List[NbUsers]): Unit =
      val total = nbUsersList.sum
      (minRating.value to 2800 by percentileOf.group).toList
        .zip(nbUsersList)
        .foldLeft(0) { case (prev, (rating, nbUsers)) =>
          val acc = prev + nbUsers
          PerfType(perfId).foreach: pt =>
            lila.mon.rating.distribution(pt.key.value, rating).update(prev.toDouble / total)
          acc
        }
