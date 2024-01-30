package lila.relay

import lila.db.dsl.{ *, given }

final private class RelayTourRepo(val coll: Coll)(using Executor):

  def setSyncedNow(tour: RelayTour): Funit =
    coll.updateField($id(tour.id), "syncedAt", nowInstant).void

  def setActive(tourId: RelayTour.Id, active: Boolean): Funit =
    coll.updateField($id(tourId), "active", active).void

  def lookup(local: String) = $lookup.simple(coll, "tour", local, "_id")

  def countByOwner(owner: UserId): Fu[Int] =
    coll.countSel(selectors.ownerId(owner))

  def subscribers(tid: RelayTour.Id): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("subscribers", $id(tid))

  def setSubscribed(tid: RelayTour.Id, uid: UserId, isSubscribed: Boolean): Funit =
    coll.update
      .one($id(tid), if isSubscribed then $addToSet("subscribers" -> uid) else $pull("subscribers" -> uid))
      .void

  def isSubscribed(tid: RelayTour.Id, uid: UserId): Fu[Boolean] =
    coll.exists($doc($id(tid), "subscribers" -> uid))

  def hasNotified(rt: RelayRound.WithTour): Fu[Boolean] =
    coll.exists($doc($id(rt.tour.id), "notified" -> rt.round.id))

  def setNotified(rt: RelayRound.WithTour): Funit =
    coll.update.one($id(rt.tour.id), $addToSet("notified" -> rt.round.id)).void

  def delete(tour: RelayTour): Funit =
    coll.delete.one($id(tour.id)).void

  private[relay] object selectors:
    val official           = $doc("tier" $exists true)
    val active             = $doc("active" -> true)
    val inactive           = $doc("active" -> false)
    def ownerId(u: UserId) = $doc("ownerId" -> u)
    val officialActive     = official ++ active
    val officialInactive   = official ++ inactive
