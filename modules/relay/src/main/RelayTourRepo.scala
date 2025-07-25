package lila.relay

import java.time.YearMonth

import lila.db.dsl.{ *, given }
import lila.relay.BSONHandlers.given
import lila.core.study.Visibility

final private class RelayTourRepo(val coll: Coll)(using Executor):
  import RelayTourRepo.*
  import RelayTour.IdName

  def exists(id: RelayRoundId): Fu[Boolean] = coll.exists($id(id))

  def byId(tourId: RelayTourId): Fu[Option[RelayTour]] = coll.byId[RelayTour](tourId)

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
    coll.countSel(selectors.ownerId(owner) ++ publicOnly.so(selectors.vis.public))

  def subscribers(tid: RelayTourId): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("subscribers", $id(tid))

  def setSubscribed(tid: RelayTourId, uid: UserId, isSubscribed: Boolean): Funit =
    coll.update
      .one($id(tid), if isSubscribed then $addToSet("subscribers" -> uid) else $pull("subscribers" -> uid))
      .void

  def isSubscribed(tid: RelayTourId, uid: UserId): Fu[Boolean] =
    coll.secondary.exists($doc($id(tid), "subscribers" -> uid))

  def countBySubscriberId(uid: UserId): Fu[Int] =
    coll.countSel(selectors.subscriberId(uid))

  private[relay] def hasNotified(rt: RelayRound.WithTour): Fu[Boolean] =
    coll.exists($doc($id(rt.tour.id), "notified" -> rt.round.id))

  def setNotified(rt: RelayRound.WithTour): Funit =
    coll.update.one($id(rt.tour.id), $addToSet("notified" -> rt.round.id)).void

  def delete(tour: RelayTour): Funit =
    coll.delete.one($id(tour.id)).void

  def idNames(ids: List[RelayTourId]): Fu[List[IdName]] =
    coll.byOrderedIds[IdName, RelayTourId](ids, $doc("name" -> true).some)(_.id)

  def isOwnerOfAll(u: UserId, ids: List[RelayTourId]): Fu[Boolean] =
    coll.exists($doc($inIds(ids), "ownerIds".$ne(u))).not

  def info(tourId: RelayTourId): Fu[Option[RelayTour.Info]] =
    coll.primitiveOne[RelayTour.Info]($id(tourId), "info")

  def allActivePublicTours(max: Max): Fu[List[RelayTour]] =
    coll.find(selectors.officialActive).sort($sort.desc("tier")).cursor[RelayTour]().list(max.value)

  def aggregateRoundAndUnwind(
      otherColls: RelayColls,
      framework: coll.AggregationFramework.type,
      onlyKeepGroupFirst: Boolean = true,
      roundPipeline: Option[List[Bdoc]] = None
  ) =
    aggregateRound(otherColls, framework, onlyKeepGroupFirst, roundPipeline) :::
      List(framework.UnwindField("round"))

  def aggregateRound(
      otherColls: RelayColls,
      framework: coll.AggregationFramework.type,
      onlyKeepGroupFirst: Boolean = true,
      roundPipeline: Option[List[Bdoc]] = None
  ) =
    onlyKeepGroupFirst.so(
      List(
        framework.PipelineOperator(group.firstLookup(otherColls.group)),
        framework.Match(group.firstFilter)
      )
    ) ::: List(
      framework.PipelineOperator(
        $lookup.pipeline(
          from = otherColls.round,
          as = "round",
          local = "_id",
          foreign = "tourId",
          pipe = roundPipeline | List(
            $doc("$sort" -> RelayRoundRepo.sort.desc),
            $doc("$limit" -> 1),
            $doc("$addFields" -> $doc("sync.log" -> $arr()))
          )
        )
      )
    )

private object RelayTourRepo:

  object group:

    // look at the groups where the tour appears.
    // only keep the tour if there is no group,
    // or if the tour is the first in the group.
    def firstLookup(groupColl: Coll) = $lookup.pipelineFull(
      from = groupColl.name,
      as = "group",
      let = $doc("tourId" -> "$_id"),
      pipe = List(
        $doc("$match" -> $doc("$expr" -> $doc("$in" -> $arr("$$tourId", "$tours")))),
        $doc:
          "$project" -> $doc(
            "_id" -> false,
            "name" -> true,
            "isFirst" -> $doc("$eq" -> $arr("$$tourId", $doc("$first" -> "$tours")))
          )
      )
    )
    val firstFilter = $doc("group.0.isFirst".$ne(false))

    def readFrom(doc: Bdoc): Option[RelayGroup.Name] = for
      garr <- doc.getAsOpt[Barr]("group")
      gdoc <- garr.getAsOpt[Bdoc](0)
      name <- gdoc.getAsOpt[RelayGroup.Name]("name")
    yield name

    def readFromOne(doc: Bdoc): Option[RelayGroup.Name] = for
      gdoc <- doc.getAsOpt[Bdoc]("group")
      name <- gdoc.getAsOpt[RelayGroup.Name]("name")
    yield name

  object selectors:
    val official = $doc("tier".$exists(true))
    object vis:
      val public = $doc("visibility" -> Visibility.public)
      val notPublic = $doc("visibility".$ne(Visibility.public))
      val `private` = $doc("visibility" -> Visibility.`private`)
    val officialPublic = official ++ vis.public
    val officialNotPublic = official ++ vis.notPublic
    val active = $doc("active" -> true)
    val inactive = $doc("active" -> false)
    def ownerId(u: UserId) = $doc("ownerIds" -> u)
    def subscriberId(u: UserId) = $doc("subscribers" -> u)
    val officialActive = officialPublic ++ active
    val officialInactive = officialPublic ++ inactive
    def inMonth(at: YearMonth) =
      val date = java.time.LocalDate.of(at.getYear, at.getMonth, 1)
      $doc("dates.start" -> $doc("$lte" -> date.plusMonths(1)), "dates.end" -> $doc("$gte" -> date))

  def readToursWithRound[A](
      as: (RelayTour, RelayRound, Option[RelayGroup.Name]) => A
  )(docs: List[Bdoc]): List[A] = for
    doc <- docs
    tour <- doc.asOpt[RelayTour]
    round <- doc.getAsOpt[RelayRound]("round")
    g = group.readFrom(doc)
  yield as(tour, round, g)
