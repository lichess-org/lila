package lila.coach

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final private class CoachRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  private[coach] object selectors {
    val listed = $doc("listed" -> true)
    val approved = $doc("approved" -> true)
    val available = $doc("available" -> true)
  }
}
