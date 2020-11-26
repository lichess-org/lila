package lila.analyse

import org.joda.time._
import reactivemongo.api.bson.{ BSONBoolean, BSONInteger }

import lila.db.dsl._
import lila.user.User

final class RequesterApi(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  private val formatter = format.DateTimeFormat.forPattern("yyyy-MM-dd")

  def save(analysis: Analysis, value: Int): Funit =
    coll.update
      .one(
        $id(analysis.uid | "anonymous"),
        $inc("total"                         -> 1) ++
          $inc(formatter.print(DateTime.now) -> value) ++
          $set("last" -> analysis.id),
        upsert = true
      )
      .void

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
