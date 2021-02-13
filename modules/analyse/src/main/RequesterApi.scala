package lila.analyse

import org.joda.time._

import lila.db.dsl._
import lila.user.User

final class RequesterApi(coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  private val formatter = format.DateTimeFormat.forPattern("yyyy-MM-dd")

  private def today = formatter.print(DateTime.now)

  def save(analysis: Analysis): Funit =
    coll.update
      .one(
        $id(analysis.uid | "anonymous"),
        $inc("total" -> 1) ++
          $inc(today -> 1) ++
          $set("last" -> analysis.id),
        upsert = true
      )
      .void

  def countToday(userId: User.ID): Fu[Int] =
    coll.primitiveOne[Int]($id(userId), today) map (~_)
}
