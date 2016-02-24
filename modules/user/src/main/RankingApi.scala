package lila.user

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework.{ Match, Project, Group, GroupField, SumField, SumValue }
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.BSON._
import lila.db.BSON.MapValue.MapHandler
import lila.memo.{ AsyncCache, MongoCache }
import lila.rating.{ Perf, PerfType }

final class RankingApi(
    coll: lila.db.Types.Coll,
    mongoCache: MongoCache.Builder,
    lightUser: String => Option[lila.common.LightUser]) {

  import RankingApi._
  private implicit val rankingBSONHandler = reactivemongo.bson.Macros.handler[Ranking]

  private type Rating = Int

  def save(userId: User.ID, perfType: Option[PerfType], perfs: Perfs): Funit =
    perfType ?? { pt =>
      save(userId, pt, perfs(pt))
    }

  def save(userId: User.ID, perfType: PerfType, perf: Perf): Funit =
    (perf.nb >= 2) ?? coll.update(BSONDocument(
      "_id" -> s"$userId:${perfType.id}"
    ), BSONDocument(
      "user" -> userId,
      "perf" -> perfType.id,
      "rating" -> perf.intRating,
      "prog" -> perf.progress,
      "stable" -> perf.established,
      "expiresAt" -> DateTime.now.plusDays(7)),
      upsert = true).void

  def topPerf(perfId: Perf.ID, nb: Int): Fu[List[User.LightPerf]] =
    PerfType.id2key(perfId) ?? { perfKey =>
      coll.find(BSONDocument("perf" -> perfId, "stable" -> true))
        .sort(BSONDocument("rating" -> -1))
        .cursor[Ranking]().collect[List](nb) map {
          _.flatMap { r =>
            lightUser(r.user).map { light =>
              User.LightPerf(
                user = light,
                perfKey = perfKey,
                rating = r.rating,
                progress = r.prog)
            }
          }
        }
    }

  object weeklyStableRanking {

    def of(userId: User.ID): Fu[Map[Perf.Key, Int]] =
      lila.common.Future.traverseSequentially(PerfType.leaderboardable) { perf =>
        cache(perf.id) map { _ get userId map (perf.key -> _) }
      } map (_.flatten.toMap)

    private val cache = AsyncCache[Perf.ID, Map[User.ID, Rating]](
      f = compute,
      timeToLive = 15 minutes)

    private def compute(perfId: Perf.ID): Fu[Map[User.ID, Rating]] = {
      val enumerator = coll.find(
        BSONDocument("perf" -> perfId, "stable" -> true),
        BSONDocument("user" -> true, "_id" -> false)
      ).sort(BSONDocument("rating" -> -1)).cursor[BSONDocument]().enumerate()
      var rank = 1
      val b = Map.newBuilder[User.ID, Rating]
      val mapBuilder: Iteratee[BSONDocument, Unit] = Iteratee.foreach { doc =>
        doc.getAs[User.ID]("user") foreach { user =>
          b += (user -> rank)
          rank = rank + 1
        }
      }
      enumerator.run(mapBuilder) inject b.result
    }
  }

  object weeklyRatingDistribution {

    private type NbUsers = Int

    def apply(perf: PerfType) = cache(perf.id)

    private val cache = mongoCache[Perf.ID, List[NbUsers]](
      prefix = "user:rating:distribution",
      f = compute,
      timeToLive = 3 hour)

    // from 800 to 2500 by Stat.group
    private def compute(perfId: Perf.ID): Fu[List[NbUsers]] =
      lila.rating.PerfType(perfId).exists(lila.rating.PerfType.leaderboardable.contains) ?? {
        coll.aggregate(
          Match(BSONDocument("perf" -> perfId)),
          List(Project(BSONDocument(
            "_id" -> false,
            "r" -> BSONDocument(
              "$subtract" -> BSONArray(
                "$rating",
                BSONDocument("$mod" -> BSONArray("$rating", Stat.group))
              )
            )
          )),
            GroupField("r")("nb" -> SumValue(1))
          )).map { res =>
            val hash = res.documents.flatMap { obj =>
              for {
                rating <- obj.getAs[Int]("_id")
                nb <- obj.getAs[NbUsers]("nb")
              } yield rating -> nb
            }.toMap
            (800 to 2500 by Stat.group).map { r =>
              hash.getOrElse(r, 0)
            }.toList
          }
      }
  }
}

object RankingApi {

  private case class Ranking(user: String, rating: Int, prog: Int)
}
