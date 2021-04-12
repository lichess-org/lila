package lila.analyse

import org.joda.time._
import reactivemongo.api.bson.{ BSONBoolean, BSONInteger }
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

final class RequesterApi(coll: Coll, cacheApi: CacheApi)(implicit ec: scala.concurrent.ExecutionContext) {

  private val formatter = format.DateTimeFormat.forPattern("yyyy-MM-dd")

  private[analyse] val requesterCache =
    cacheApi.notLoadingSync[Analysis.ID, lila.user.User.ID](256, "analyse.requester") {
      _.expireAfterWrite(10 minutes).build()
    }

  def save(analysis: Analysis, playerIds: List[User.ID]): Funit =
    requesterCache.getIfPresent(analysis.id) ?? { requester =>
      val cost = if (playerIds has requester) 1 else 2
      coll.update
        .one(
          $id(requester),
          $inc("total"                         -> 1) ++
            $inc(formatter.print(DateTime.now) -> cost) ++
            $set("last" -> analysis.id),
          upsert = true
        )
        .void
    }

  def countTodayAndThisWeek(userId: User.ID): Fu[(Int, Int)] = {
    val now = DateTime.now
    coll
      .one(
        $id(userId),
        $doc {
          (7 to 0 by -1).toList.map(now.minusDays).map(formatter.print).map(_ -> BSONBoolean(true))
        }
      )
      .map { doc =>
        val daily = doc.flatMap(_ int formatter.print(now))
        val weekly = doc ?? {
          _.values.foldLeft(0) {
            case (acc, BSONInteger(v)) => acc + v
            case (acc, _)              => acc
          }
        }
        (~daily, weekly)
      }
  }
}
