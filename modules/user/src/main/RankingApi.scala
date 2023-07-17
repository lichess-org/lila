package lila.user

import reactivemongo.api.bson.*
import scala.util.Success

import lila.db.AsyncCollFailingSilently
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi.*
import lila.rating.{ Glicko, Perf, PerfType }

final class RankingApi(
    coll: AsyncCollFailingSilently,
    cacheApi: lila.memo.CacheApi,
    mongoCache: lila.memo.MongoCache.Api,
    lightUser: lila.common.LightUser.Getter
)(using Executor):

  import RankingApi.*
  private given BSONDocumentHandler[Ranking] = Macros.handler[Ranking]

  def save(user: User, perfType: PerfType, perfs: UserPerfs): Funit =
    save(user, perfType, perfs(perfType))

  def save(user: User, perfType: PerfType, perf: Perf): Funit =
    (user.rankable && perf.nb >= 2 && PerfType.isLeaderboardable(perfType)) so coll:
      _.update
        .one(
          $id(makeId(user.id, perfType)),
          $doc(
            "perf"      -> perfType.id,
            "rating"    -> perf.intRating,
            "prog"      -> perf.progress,
            "stable"    -> perf.rankable(PerfType variantOf perfType),
            "expiresAt" -> nowInstant.plusDays(7)
          ),
          upsert = true
        )
        .void

  def remove(userId: UserId): Funit =
    coll:
      _.delete.one($doc("_id" $startsWith s"$userId:")).void

  private def makeId(userId: UserId, perfType: PerfType) =
    s"$userId:${perfType.id}"

  private[user] def topPerf(perfId: Perf.Id, nb: Int): Fu[List[User.LightPerf]] =
    PerfType.id2key(perfId).filter(k => PerfType(k).exists(PerfType.isLeaderboardable)) so { perfKey =>
      coll:
        _.find($doc("perf" -> perfId, "stable" -> true))
          .sort($doc("rating" -> -1))
          .cursor[Ranking]()
          .list(nb)
          .flatMap:
            _.map: r =>
              lightUser(r.user).map2: light =>
                User.LightPerf(
                  user = light,
                  perfKey = perfKey,
                  rating = r.rating,
                  progress = ~r.prog
                )
            .parallel.dmap(_.flatten)
    }

  private[user] def fetchLeaderboard(nb: Int): Fu[UserPerfs.Leaderboards] =
    for
      ultraBullet   <- topPerf(PerfType.UltraBullet.id, nb)
      bullet        <- topPerf(PerfType.Bullet.id, nb)
      blitz         <- topPerf(PerfType.Blitz.id, nb)
      rapid         <- topPerf(PerfType.Rapid.id, nb)
      classical     <- topPerf(PerfType.Classical.id, nb)
      chess960      <- topPerf(PerfType.Chess960.id, nb)
      kingOfTheHill <- topPerf(PerfType.KingOfTheHill.id, nb)
      threeCheck    <- topPerf(PerfType.ThreeCheck.id, nb)
      antichess     <- topPerf(PerfType.Antichess.id, nb)
      atomic        <- topPerf(PerfType.Atomic.id, nb)
      horde         <- topPerf(PerfType.Horde.id, nb)
      racingKings   <- topPerf(PerfType.RacingKings.id, nb)
      crazyhouse    <- topPerf(PerfType.Crazyhouse.id, nb)
    yield UserPerfs.Leaderboards(
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

  object weeklyStableRanking:

    private type Rank = Int

    def of(userId: UserId): Map[PerfType, Rank] =
      cache.getUnit.value match
        case Some(Success(all)) =>
          all.flatMap: (pt, ranking) =>
            ranking get userId map (pt -> _)
        case _ => Map.empty

    private val cache = cacheApi.unit[Map[PerfType, Map[UserId, Rank]]]:
      _.refreshAfterWrite(15 minutes).buildAsyncFuture: _ =>
        PerfType.leaderboardable
          .traverse: pt =>
            compute(pt).dmap(pt -> _)
          .map(_.toMap)
          .chronometer
          .logIfSlow(500, logger.branch("ranking"))(_ => "slow weeklyStableRanking")
          .result

    private def compute(pt: PerfType): Fu[Map[UserId, Rank]] = coll:
      _.find(
        $doc("perf" -> pt.id, "stable" -> true),
        $doc("_id" -> true).some
      )
        .sort($doc("rating" -> -1))
        .cursor[Bdoc]()
        .fold(1 -> Map.newBuilder[UserId, Rank]) { case (state @ (rank, b), doc) =>
          doc
            .string("_id")
            .fold(state): id =>
              val user = UserId(id.takeWhile(':' !=))
              b += (user -> rank)
              (rank + 1) -> b
        }
        .map(_._2.result())

  object weeklyRatingDistribution:

    private type NbUsers = Int

    def apply(pt: PerfType) = cache.get(pt.id)

    private val cache = mongoCache[Perf.Id, List[NbUsers]](
      PerfType.leaderboardable.size,
      "user:rating:distribution",
      179 minutes,
      _.toString
    ): loader =>
      _.refreshAfterWrite(180 minutes).buildAsyncFuture:
        loader(compute)

    // from 600 to 2800 by Stat.group
    private def compute(perfId: Perf.Id): Fu[List[NbUsers]] =
      lila.rating.PerfType(perfId).exists(lila.rating.PerfType.leaderboardable.contains) so coll:
        _.aggregateList(maxDocs = Int.MaxValue): framework =>
          import framework.*
          Match($doc("perf" -> perfId)) -> List(
            Project(
              $doc(
                "_id" -> false,
                "r" -> $doc(
                  "$subtract" -> $arr(
                    "$rating",
                    $doc("$mod" -> $arr("$rating", Stat.group))
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
                nb     <- obj.getAsOpt[NbUsers]("nb")
              yield rating -> nb
            .to(Map)
          (Glicko.minRating.value to 2800 by Stat.group)
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
    private def monitorRatingDistribution(perfId: Perf.Id)(nbUsersList: List[NbUsers]): Unit =
      val total = nbUsersList.sum
      (Stat.minRating.value to 2800 by Stat.group).toList
        .zip(nbUsersList)
        .foldLeft(0) { case (prev, (rating, nbUsers)) =>
          val acc = prev + nbUsers
          PerfType(perfId).foreach: pt =>
            lila.mon.rating.distribution(pt.key.value, rating).update(prev.toDouble / total)
          acc
        }

object RankingApi:

  private case class Ranking(_id: String, rating: IntRating, prog: Option[IntRatingDiff]):
    def user = UserId(_id.takeWhile(':' !=))
