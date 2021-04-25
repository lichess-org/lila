package lila.relay

import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final private class RelayTourRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def lookup(local: String) = $lookup.simple(coll, "tour", local, "_id")

  private[relay] object selectors {
    val official = $doc("official" -> true)
    val active   = $doc("active" -> true)
    val inactive = $doc("active" -> false)
  }
}
