package lila.user

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.common.{ AtMost, Every }
import lila.db.dsl._
import lila.memo.PeriodicRefreshCache
import lila.rating.{ Glicko, Perf, PerfType }

final class RankingApi(
    userRepo: UserRepo,
    coll: Coll,
    mongoCache: lila.memo.MongoCache.Builder,
    lightUser: lila.common.LightUser.Getter
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

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

  def remove(userId: User.ID): Funit = userRepo byId userId flatMap {
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
    s"${userId}:${perfType.id}"

  private[user] def topPerf(perfId: Perf.ID, nb: Int): Fu[List[User.LightPerf]] =
    PerfType.id2key(perfId) ?? { perfKey =>
      coll.ext
        .find($doc("perf" -> perfId, "stable" -> true))
        .sort($doc("rating" -> -1))
        .cursor[Ranking](readPreference = ReadPreference.secondaryPreferred)
        .gather[List](nb) flatMap { res =>
        res
          .map { r =>
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
          }
          .sequenceFu
          .map(_.flatten)
      }
    }

  def fetchLeaderboard(nb: Int): Fu[Perfs.Leaderboards] =
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

    def of(userId: User.ID): Map[PerfType, Rank] =
      cache.get flatMap {
        case (pt, ranking) => ranking get userId map (pt -> _)
      } toMap

    private val cache = new PeriodicRefreshCache[Map[PerfType, Map[User.ID, Rank]]](
      every = Every(15 minutes),
      atMost = AtMost(3 minutes),
      f = () =>
        lila.common.Future
          .traverseSequentially(PerfType.leaderboardable) { pt =>
            compute(pt) map (pt -> _)
          }
          .map(_.toMap),
      default = Map.empty,
      initialDelay = 1 minute
    )

    private def compute(pt: PerfType): Fu[Map[User.ID, Rank]] =
      coll.ext
        .find(
          $doc("perf" -> pt.id, "stable" -> true),
          $doc("_id"  -> true)
        )
        .sort($doc("rating" -> -1))
        .cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred)
        .fold(1 -> Map.newBuilder[User.ID, Rank]) {
          case (state @ (rank, b), doc) =>
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

    def apply(perf: PerfType) = cache(perf.id)

    private val cache = mongoCache[Perf.ID, List[NbUsers]](
      prefix = "user:rating:distribution",
      f = compute,
      timeToLive = 3 hour,
      keyToString = _.toString
    )

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
          } addEffect monitorRatingDistribution(perfId) _
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
      val total = nbUsersList.foldLeft(0)(_ + _)
      (Stat.minRating to 2800 by Stat.group).toList.zip(nbUsersList).foldLeft(0) {
        case (prev, (rating, nbUsers)) =>
          val acc = prev + nbUsers
          PerfType(perfId) foreach { pt =>
            lila.mon.rating.distribution(pt.key, rating).update(acc.toDouble / total)
          }
          acc
      }
    }
  }
}

object RankingApi {

  private case class Ranking(_id: String, rating: Int, prog: Option[Int]) {
    def user = _id.takeWhile(':' !=)
  }
}
