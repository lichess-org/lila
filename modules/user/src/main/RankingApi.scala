package lila.user

import org.joda.time.DateTime
import play.api.libs.iteratee._
import reactivemongo.bson._
import scala.concurrent.duration._

import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.BSON.MapValue.MapHandler
import lila.memo.AsyncCache
import lila.rating.{ Perf, PerfType }

final class RankingApi(coll: lila.db.Types.Coll) {

  private type Rating = Int
  private type PerfId = Int

  def save(userId: User.ID, perfType: Option[PerfType], perfs: Perfs): Funit =
    perfType ?? { pt =>
      save(userId, pt, perfs(pt))
    }

  def save(userId: User.ID, perfType: PerfType, perf: Perf): Funit =
    (perf.nb >= 2) ?? coll.update(BSONDocument(
      "_id" -> s"$user:${perfType.id}"
    ), BSONDocument(
      "user" -> userId,
      "perf" -> perfType.id,
      "rating" -> perf.intRating,
      "stable" -> perf.established,
      "date" -> DateTime.now),
      upsert = true).void

  object weeklyStableRanking {

    def of(userId: User.ID): Fu[Map[Perf.Key, Int]] =
      lila.common.Future.traverseSequentially(PerfType.leaderboardable) { perf =>
        cache(perf.id) map { _ get userId map (perf.key -> _) }
      } map (_.flatten.toMap)

    private val cache = AsyncCache[PerfId, Map[User.ID, Rating]](
      f = compute,
      timeToLive = 15 minutes)

    private def compute(perfId: PerfId): Fu[Map[User.ID, Rating]] = {
      val enumerator = coll.find(
        BSONDocument("perf" -> perfId),
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

  object monthlyRatingDistribution {

    private val cache = mongoCache[Perf.Key, List[NbUsers]](
      prefix = "user:rating:distribution",
      f = compute,
      timeToLive = 3 hour)

    // from 800 to 2500 by Stat.group
    def compute(perf: Perf.Key): Fu[List[Int]] =
      lila.rating.PerfType(perf).exists(lila.rating.PerfType.leaderboardable.contains) ?? {
        val field = s"perfs.$perf"
        coll.aggregate(
          Match(BSONDocument(
            s"$field.la" -> BSONDocument("$gt" -> DateTime.now.minusMonths(1)),
            s"$field.nb" -> BSONDocument("$gt" -> 2)
          )),
          List(Project(BSONDocument(
            "_id" -> false,
            "r" -> BSONDocument(
              "$subtract" -> BSONArray(
                s"$$$field.gl.r",
                BSONDocument("$mod" -> BSONArray(s"$$$field.gl.r", Stat.group))
              )
            )
          )),
            GroupField("r")("nb" -> SumValue(1))
          )).map { res =>
            val hash = res.documents.flatMap { obj =>
              for {
                rating <- obj.getAs[Double]("_id")
                nb <- obj.getAs[Int]("nb")
              } yield rating.toInt -> nb
            }.toMap
            (800 to 2500 by Stat.group).map { r =>
              hash.getOrElse(r, 0)
            }.toList
          }
      }
  }
}
