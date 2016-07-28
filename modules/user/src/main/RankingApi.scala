package lila.user

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api.{ Cursor, ReadPreference }
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Match, Project, Group, GroupField, SumField, SumValue }
import reactivemongo.api.Cursor
import reactivemongo.api.ReadPreference
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.BSON.MapValue.MapHandler
import lila.db.dsl._
import lila.memo.{ AsyncCache, MongoCache }
import lila.rating.{ Perf, PerfType }

final class RankingApi(
    coll: Coll,
    mongoCache: MongoCache.Builder,
    lightUser: String => Option[lila.common.LightUser]) {

  import RankingApi._
  private implicit val rankingBSONHandler = Macros.handler[Ranking]

  def save(userId: User.ID, perfType: Option[PerfType], perfs: Perfs): Funit =
    perfType ?? { pt =>
      save(userId, pt, perfs(pt))
    }

  def save(userId: User.ID, perfType: PerfType, perf: Perf): Funit =
    (perf.nb >= 2) ?? coll.update($id(makeId(userId, perfType)), $doc(
      "user" -> userId,
      "perf" -> perfType.id,
      "rating" -> perf.intRating,
      "prog" -> perf.progress,
      "stable" -> perf.established,
      "expiresAt" -> DateTime.now.plusDays(7)),
      upsert = true).void

  def remove(userId: User.ID): Funit = UserRepo byId userId flatMap {
    _ ?? { user =>
      coll.remove($inIds(
        PerfType.leaderboardable.filter { pt =>
          user.perfs(pt).nonEmpty
        }.map { makeId(user.id, _) }
      )).void
    }
  }

  private def makeId(userId: User.ID, perfType: PerfType) =
    s"${userId}:${perfType.id}"

  private[user] def topPerf(perfId: Perf.ID, nb: Int): Fu[List[User.LightPerf]] =
    PerfType.id2key(perfId) ?? { perfKey =>
      coll.find($doc("perf" -> perfId, "stable" -> true))
        .sort($doc("rating" -> -1))
        .cursor[Ranking](readPreference = ReadPreference.secondaryPreferred)
        .gather[List](nb) map {
          _.flatMap { r =>
            lightUser(r.user).map { light =>
              User.LightPerf(
                user = light,
                perfKey = perfKey,
                rating = r.rating,
                progress = ~r.prog)
            }
          }
        }
    }

  object weeklyStableRanking {

    private type Rank = Int

    def of(userId: User.ID): Fu[Map[Perf.Key, Int]] =
      lila.common.Future.traverseSequentially(PerfType.leaderboardable) { perf =>
        cache(perf.id) map { _ get userId map (perf.key -> _) }
      } map (_.flatten.toMap)

    private val cache = AsyncCache[Perf.ID, Map[User.ID, Rank]](
      f = compute,
      timeToLive = 15 minutes)

    private def compute(perfId: Perf.ID): Fu[Map[User.ID, Rank]] =
      coll.find(
        $doc("perf" -> perfId, "stable" -> true),
        $doc("user" -> true, "_id" -> false)
      ).sort($doc("rating" -> -1)).cursor[Bdoc](readPreference = ReadPreference.secondaryPreferred).
        fold(1 -> Map.newBuilder[User.ID, Rank]) {
          case (state@(rank, b), doc) =>
            doc.getAs[User.ID]("user").fold(state) { user =>
              b += (user -> rank)
              (rank + 1) -> b
            }
        }.map(_._2.result())
  }

  object weeklyRatingDistribution {

    private type NbUsers = Int

    def apply(perf: PerfType) = cache(perf.id)

    private val cache = mongoCache[Perf.ID, List[NbUsers]](
      prefix = "user:rating:distribution",
      f = compute,
      timeToLive = 3 hour,
      keyToString = _.toString)

    // from 800 to 2500 by Stat.group
    private def compute(perfId: Perf.ID): Fu[List[NbUsers]] =
      lila.rating.PerfType(perfId).exists(lila.rating.PerfType.leaderboardable.contains) ?? {
        coll.aggregate(
          Match($doc("perf" -> perfId)),
          List(Project($doc(
            "_id" -> false,
            "r" -> $doc(
              "$subtract" -> $arr(
                "$rating",
                $doc("$mod" -> $arr("$rating", Stat.group))
              )
            )
          )),
            GroupField("r")("nb" -> SumValue(1))
          )).map { res =>
            val hash = res.firstBatch.flatMap { obj =>
              for {
                rating <- obj.getAs[Int]("_id")
                nb <- obj.getAs[NbUsers]("nb")
              } yield rating -> nb
            }.toMap
            (800 to 2800 by Stat.group).map { r =>
              hash.getOrElse(r, 0)
            }.toList
          }
      }
  }
}

object RankingApi {

  private case class Ranking(user: String, rating: Int, prog: Option[Int])
}
