package lila.user

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi._
import lila.rating.{ Glicko, Perf, PerfType }

final class RankingApi(
    userRepo: UserRepo,
    coll: Coll,
    cacheApi: lila.memo.CacheApi,
    mongoCache: lila.memo.MongoCache.Api,
    lightUser: lila.common.LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import RankingApi._
  implicit private val rankingBSONHandler = Macros.handler[Ranking]

  def save(user: User, perfType: Option[PerfType], perfs: Perfs): Funit =
    perfType ?? { pt =>
      save(user, pt, perfs(pt))
    }

  def save(user: User, perfType: PerfType, perf: Perf): Funit =
    (user.rankable && perf.nb >= 2) ?? coll.update
      .one(
        $id(makeId(user.id, perfType)),
        $doc(
          "perf"      -> perfType.id,
          "rating"    -> perf.intRating,
          "prog"      -> perf.progress,
          "stable"    -> perf.rankable(PerfType variantOf perfType),
          "expiresAt" -> DateTime.now.plusDays(7)
        ),
        upsert = true
      )
      .void

  def remove(userId: User.ID): Funit =
    userRepo byId userId flatMap {
      _ ?? { user =>
        coll.delete
          .one(
            $inIds(
              PerfType.leaderboardable
                .filter { pt =>
                  user.perfs(pt).nonEmpty
                }
                .map { makeId(user.id, _) }
            )
          )
          .void
      }
    }

  private def makeId(userId: User.ID, perfType: PerfType) =
    s"$userId:${perfType.id}"

  private[user] def topPerf(perfId: Perf.ID, nb: Int): Fu[List[User.LightPerf]] =
    PerfType.id2key(perfId) ?? { perfKey =>
      coll
        .find($doc("perf" -> perfId, "stable" -> true))
        .sort($doc("rating" -> -1))
        .cursor[Ranking](ReadPreference.secondaryPreferred)
        .list(nb)
        .flatMap {
          _.map { r =>
            lightUser(r.user).map {
              _ map { light =>
                User.LightPerf(
                  user = light,
                  perfKey = perfKey,
                  rating = r.rating,
                  progress = ~r.prog
                )
              }
            }
          }.sequenceFu
            .dmap(_.flatten)
        }
    }

  private[user] def fetchLeaderboard(nb: Int): Fu[Perfs.Leaderboards] =
    for {
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
    } yield Perfs.Leaderboards(
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

  object weeklyStableRanking {

    private type Rank = Int

    def of(userId: User.ID): Fu[Map[PerfType, Rank]] =
      cache.getUnit map { all =>
        all.flatMap { case (pt, ranking) =>
          ranking get userId map (pt -> _)
        }
      }

    private val cache = cacheApi.unit[Map[PerfType, Map[User.ID, Rank]]] {
      _.refreshAfterWrite(15 minutes)
        .buildAsyncFuture { _ =>
          lila.common.Future
            .linear(PerfType.leaderboardable) { pt =>
              compute(pt) dmap (pt -> _)
            }
            .dmap(_.toMap)
        }
    }

    private def compute(pt: PerfType): Fu[Map[User.ID, Rank]] =
      coll
        .find(
          $doc("perf" -> pt.id, "stable" -> true),
          $doc("_id" -> true).some
        )
        .sort($doc("rating" -> -1))
        .cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred)
        .fold(1 -> Map.newBuilder[User.ID, Rank]) { case (state @ (rank, b), doc) =>
          doc.string("_id").fold(state) { id =>
            val user = id takeWhile (':' !=)
            b += (user -> rank)
            (rank + 1) -> b
          }
        }
        .map(_._2.result())
  }

  object weeklyRatingDistribution {

    private type NbUsers = Int

    def apply(pt: PerfType) = cache.get(pt.id)

    private val cache = mongoCache[Perf.ID, List[NbUsers]](
      PerfType.leaderboardable.size,
      "user:rating:distribution",
      179 minutes,
      _.toString
    ) { loader =>
      _.refreshAfterWrite(180 minutes)
        .buildAsyncFuture {
          loader(compute)
        }
    }

    // from 600 to 2800 by Stat.group
    private def compute(perfId: Perf.ID): Fu[List[NbUsers]] =
      lila.rating.PerfType(perfId).exists(lila.rating.PerfType.leaderboardable.contains) ?? {
        coll
          .aggregateList(
            maxDocs = Int.MaxValue,
            ReadPreference.secondaryPreferred
          ) { framework =>
            import framework._
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
          }
          .map { res =>
            val hash: Map[Int, NbUsers] = res.view
              .flatMap { obj =>
                for {
                  rating <- obj.int("_id")
                  nb     <- obj.getAsOpt[NbUsers]("nb")
                } yield rating -> nb
              }
              .to(Map)
            (Glicko.minRating to 2800 by Stat.group).map { r =>
              hash.getOrElse(r, 0)
            }.toList
          } addEffect monitorRatingDistribution(perfId)
      }

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
    private def monitorRatingDistribution(perfId: Perf.ID)(nbUsersList: List[NbUsers]): Unit = {
      val total = nbUsersList.sum
      (Stat.minRating to 2800 by Stat.group).toList
        .zip(nbUsersList)
        .foldLeft(0) { case (prev, (rating, nbUsers)) =>
          val acc = prev + nbUsers
          PerfType(perfId) foreach { pt =>
            lila.mon.rating.distribution(pt.key, rating).update(prev.toDouble / total)
          }
          acc
        }
        .unit
    }
  }
}

object RankingApi {

  private case class Ranking(_id: String, rating: Int, prog: Option[Int]) {
    def user = _id.takeWhile(':' !=)
  }
}
