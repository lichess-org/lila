package lila.relay

import lila.db.dsl.{ *, given }

final private class RelayTourRepo(val coll: Coll)(using Executor):

  def setSyncedNow(tour: RelayTour): Funit =
    coll.updateField($id(tour.id), "syncedAt", nowInstant).void

  def setActive(tourId: RelayTour.Id, active: Boolean): Funit =
    coll.updateField($id(tourId), "active", active).void

  def lookup(local: String) = $lookup.simple(coll, "tour", local, "_id")

  private[relay] object selectors:
    val official         = $doc("tier" $exists true)
    val active           = $doc("active" -> true)
    val inactive         = $doc("active" -> false)
    val officialActive   = official ++ active
    val officialInactive = official ++ inactive
