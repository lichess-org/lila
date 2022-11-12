package lila.relay

import org.joda.time.DateTime
import reactivemongo.api.bson.*
import reactivemongo.api.ReadPreference

import lila.db.dsl.{ *, given }

final private class RelayTourRepo(val coll: Coll)(using ec: scala.concurrent.ExecutionContext):

  import BSONHandlers.given

  def setSyncedNow(tour: RelayTour): Funit =
    coll.updateField($id(tour.id), "syncedAt", DateTime.now).void

  def setActive(tourId: RelayTour.Id, active: Boolean): Funit =
    coll.updateField($id(tourId), "active", active).void

  def lookup(local: String) = $lookup.simple(coll, "tour", local, "_id")

  private[relay] object selectors:
    val official         = $doc("tier" $exists true)
    val active           = $doc("active" -> true)
    val inactive         = $doc("active" -> false)
    val officialActive   = official ++ active
    val officialInactive = official ++ inactive
