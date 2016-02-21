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
    (perf.nb >= 30 && perf.glicko.established) ?? coll.update(BSONDocument(
      "_id" -> makeId(userId, perfType)
    ), BSONDocument(
      "user" -> userId,
      "perf" -> perfType.id,
      "rating" -> perf.intRating,
      "expiresAt" -> DateTime.now.plusDays(7)),
      upsert = true).void

  def getAll(userId: User.ID): Fu[Map[Perf.Key, Int]] =
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

  private def makeId(user: User.ID, perfType: PerfType) =
    s"$user:${perfType.id}"
}
