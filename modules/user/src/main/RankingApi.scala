package lila.user

import reactivemongo.api.bson.*
import scala.util.Success
import chess.{ IntRating, ByColor }
import chess.rating.IntRatingDiff

import lila.core.perf.{ PerfId, UserWithPerfs }
import lila.core.user.LightPerf
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.rating.GlickoExt.rankable
import lila.rating.PerfType
import scalalib.paginator.Paginator

final class RankingApi(
    c: lila.db.AsyncCollFailingSilently,
    cacheApi: lila.memo.CacheApi,
    lightUser: lila.core.LightUser.Getter
)(using Executor, Scheduler)
    extends lila.core.user.RankingRepo(c):

  import RankingApi.*
  private given BSONDocumentHandler[Ranking] = Macros.handler[Ranking]

  def save(us: ByColor[UserWithPerfs], perfType: PerfType): Funit =
    us.toList.parallelVoid: u =>
      save(u.user, perfType, u.perfs(perfType))

  def save(user: User, perfType: PerfType, perf: Perf): Funit =
    (user.rankable && perf.nb >= 2 && lila.rating.PerfType.isLeaderboardable(perfType)).so:
      coll:
        _.update
          .one(
            $id(makeId(user.id, perfType)),
            $doc(
              "perf" -> perfType.id,
              "rating" -> perf.intRating,
              "prog" -> perf.progress,
              "stable" -> perf.glicko.rankable(lila.rating.PerfType.variantOf(perfType)),
              "expiresAt" -> nowInstant.plusDays(7)
            ),
            upsert = true
          )
          .void

  def remove(userId: UserId): Funit =
    coll:
      _.delete.one($doc("_id".$startsWith(s"$userId:"))).void

  private def makeId(userId: UserId, perfType: PerfType) = s"$userId:${perfType.id}"

  private[user] object topPerf:
    val maxPerPage = MaxPerPage(100)

    def pager(perf: PerfKey, page: Int): Fu[Paginator[LightPerf]] =
      Paginator(
        adapter = new:
          def nbResults = fuccess(500_000)
          def slice(offset: Int, length: Int): Fu[List[LightPerf]] = fetchLightPerfs(perf, length, offset)
        ,
        currentPage = page,
        maxPerPage = maxPerPage
      )

    private[RankingApi] def fetchLightPerfs(perf: PerfKey, nb: Int, skip: Int = 0): Fu[List[LightPerf]] =
      lila.rating.PerfType
        .isLeaderboardable(perf)
        .so:
          coll:
            _.find($doc("perf" -> perf.id, "stable" -> true))
              .sort($doc("rating" -> -1))
              .skip(skip)
              .cursor[Ranking](ReadPref.sec)
              .list(nb)
              .flatMap:
                _.parallel: r =>
                  lightUser(r.user).map2:
                    LightPerf(_, perf, r.rating, ~r.prog)
                .dmap(_.flatten)

  private[user] def fetchLeaderboard(nb: Int): Fu[lila.rating.UserPerfs.Leaderboards] =
    for
      ultraBullet <- topPerf.fetchLightPerfs(PerfKey.ultraBullet, nb)
      bullet <- topPerf.fetchLightPerfs(PerfKey.bullet, nb)
      blitz <- topPerf.fetchLightPerfs(PerfKey.blitz, nb)
      rapid <- topPerf.fetchLightPerfs(PerfKey.rapid, nb)
      classical <- topPerf.fetchLightPerfs(PerfKey.classical, nb)
      chess960 <- topPerf.fetchLightPerfs(PerfKey.chess960, nb)
      kingOfTheHill <- topPerf.fetchLightPerfs(PerfKey.kingOfTheHill, nb)
      threeCheck <- topPerf.fetchLightPerfs(PerfKey.threeCheck, nb)
      antichess <- topPerf.fetchLightPerfs(PerfKey.antichess, nb)
      atomic <- topPerf.fetchLightPerfs(PerfKey.atomic, nb)
      horde <- topPerf.fetchLightPerfs(PerfKey.horde, nb)
      racingKings <- topPerf.fetchLightPerfs(PerfKey.racingKings, nb)
      crazyhouse <- topPerf.fetchLightPerfs(PerfKey.crazyhouse, nb)
    yield lila.rating.UserPerfs.Leaderboards(
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

  private[user] object weeklyStableRanking:

    private type Rank = Int

    def of(userId: UserId): Map[PerfKey, Rank] =
      cache.getUnit.value match
        case Some(Success(all)) =>
          all.flatMap: (pt, ranking) =>
            ranking.get(userId).map(pt -> _)
        case _ => Map.empty

    private val cache = cacheApi.unit[Map[PerfKey, Map[UserId, Rank]]]:
      _.refreshAfterWrite(10.minutes).buildAsyncTimeout(2.minutes): _ =>
        lila.rating.PerfType.leaderboardable
          .sequentially: perf =>
            computeAggregate(perf).chronometer
              .logIfSlow(500, logger.branch("ranking"))(_ => s"slow weeklyStableRanking for $perf")
              .result
              .monSuccess(_.user.weeklyStableRanking(perf))
              .dmap(perf -> _)
          .map(_.toMap)
          .chronometer
          .logIfSlow(5000, logger.branch("ranking"))(_ => "slow weeklyStableRanking")
          .result

    private def computeAggregate(pt: PerfType): Fu[Map[UserId, Rank]] = coll:
      _.aggregateOne(_.sec): framework =>
        import framework.*
        Match($doc("perf" -> pt.id, "stable" -> true)) -> List(
          Sort(Descending("rating")),
          Group(BSONNull)("all" -> Push($doc("$first" -> $doc("$split" -> $arr("$_id", ":")))))
        )
      .map:
        _.flatMap(_.getAsOpt[BSONArray]("all")).so:
          _.values.view.zipWithIndex
            .collect:
              case (BSONString(id), index) => UserId(id) -> (index + 1)
            .toMap

object RankingApi:

  private case class Ranking(_id: String, rating: IntRating, prog: Option[IntRatingDiff]):
    def user = UserId(_id.takeWhile(':' !=))
