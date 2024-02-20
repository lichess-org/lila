package lila.relay

import lila.db.dsl.{ *, given }

final private class RelayTourRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given
  import RelayTourRepo.*
  import RelayTour.{ Id, IdName }

  def setSyncedNow(tour: RelayTour): Funit =
    coll.updateField($id(tour.id), "syncedAt", nowInstant).void

  def setActive(tourId: Id, active: Boolean): Funit =
    coll.updateField($id(tourId), "active", active).void

  def lookup(local: String) = $lookup.simple(coll, "tour", local, "_id")

  def countByOwner(owner: UserId): Fu[Int] =
    coll.countSel(selectors.ownerId(owner))

  def subscribers(tid: Id): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("subscribers", $id(tid))

  def setSubscribed(tid: Id, uid: UserId, isSubscribed: Boolean): Funit =
    coll.update
      .one($id(tid), if isSubscribed then $addToSet("subscribers" -> uid) else $pull("subscribers" -> uid))
      .void

  def isSubscribed(tid: Id, uid: UserId): Fu[Boolean] =
    coll.exists($doc($id(tid), "subscribers" -> uid))

  def countBySubscriberId(uid: UserId): Fu[Int] =
    coll.countSel(selectors.subscriberId(uid))

  def hasNotified(rt: RelayRound.WithTour): Fu[Boolean] =
    coll.exists($doc($id(rt.tour.id), "notified" -> rt.round.id))

  def setNotified(rt: RelayRound.WithTour): Funit =
    coll.update.one($id(rt.tour.id), $addToSet("notified" -> rt.round.id)).void

  def delete(tour: RelayTour): Funit =
    coll.delete.one($id(tour.id)).void

  def idNames(ids: List[Id]): Fu[List[IdName]] =
    coll.byOrderedIds[IdName, Id](ids, $doc("name" -> true).some)(_.id)

private object RelayTourRepo:
  object selectors:
    val official                = $doc("tier" $exists true)
    val active                  = $doc("active" -> true)
    val inactive                = $doc("active" -> false)
    def ownerId(u: UserId)      = $doc("ownerId" -> u)
    def subscriberId(u: UserId) = $doc("subscribers" -> u)
    val officialActive          = official ++ active
    val officialInactive        = official ++ inactive
