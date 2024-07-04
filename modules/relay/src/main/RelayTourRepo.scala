package lila.relay

import lila.db.dsl.{ *, given }

final private class RelayTourRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given
  import RelayTourRepo.*
  import RelayTour.IdName

  def setSyncedNow(tour: RelayTour): Funit =
    coll.updateField($id(tour.id), "syncedAt", nowInstant).void

  def denormalize(
      tourId: RelayTourId,
      active: Boolean,
      live: Boolean,
      dates: Option[RelayTour.Dates]
  ): Funit =
    coll.update.one($id(tourId), $set("active" -> active, "live" -> live, "dates" -> dates)).void

  def lookup(local: String) = $lookup.simple(coll, "tour", local, "_id")

  def countByOwner(owner: UserId, publicOnly: Boolean): Fu[Int] =
    coll.countSel(selectors.ownerId(owner) ++ publicOnly.so(selectors.publicTour))

  def subscribers(tid: RelayTourId): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("subscribers", $id(tid))

  def setSubscribed(tid: RelayTourId, uid: UserId, isSubscribed: Boolean): Funit =
    coll.update
      .one($id(tid), if isSubscribed then $addToSet("subscribers" -> uid) else $pull("subscribers" -> uid))
      .void

  def isSubscribed(tid: RelayTourId, uid: UserId): Fu[Boolean] =
    coll.exists($doc($id(tid), "subscribers" -> uid))

  def countBySubscriberId(uid: UserId): Fu[Int] =
    coll.countSel(selectors.subscriberId(uid))

  def hasNotified(rt: RelayRound.WithTour): Fu[Boolean] =
    coll.exists($doc($id(rt.tour.id), "notified" -> rt.round.id))

  def setNotified(rt: RelayRound.WithTour): Funit =
    coll.update.one($id(rt.tour.id), $addToSet("notified" -> rt.round.id)).void

  def delete(tour: RelayTour): Funit =
    coll.delete.one($id(tour.id)).void

  def idNames(ids: List[RelayTourId]): Fu[List[IdName]] =
    coll.byOrderedIds[IdName, RelayTourId](ids, $doc("name" -> true).some)(_.id)

  def isOwnerOfAll(u: UserId, ids: List[RelayTourId]): Fu[Boolean] =
    coll.exists($doc($inIds(ids), "ownerId".$ne(u))).not

private object RelayTourRepo:
  object selectors:
    val official                = $doc("tier".$exists(true))
    val publicTour              = $doc("tier".$ne(RelayTour.Tier.PRIVATE))
    val privateTour             = $doc("tier" -> RelayTour.Tier.PRIVATE)
    val officialPublic          = $doc("tier".$gte(RelayTour.Tier.NORMAL))
    val active                  = $doc("active" -> true)
    val inactive                = $doc("active" -> false)
    def ownerId(u: UserId)      = $doc("ownerId" -> u)
    def subscriberId(u: UserId) = $doc("subscribers" -> u)
    val officialActive          = officialPublic ++ active
    val officialInactive        = officialPublic ++ inactive
