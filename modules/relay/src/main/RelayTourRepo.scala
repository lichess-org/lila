package lila.relay

import reactivemongo.akkastream.cursorProducer
import java.time.YearMonth

import lila.db.dsl.{ *, given }
import lila.relay.BSONHandlers.given
import lila.core.study.Visibility

final private class RelayTourRepo(val coll: Coll)(using Executor):

  import RelayTourRepo.*
  import RelayTour.TourPreview

  def byId(tourId: RelayTourId): Fu[Option[RelayTour]] = coll.byIdProj[RelayTour](tourId, modelProjection)

  def setSyncedNow(tour: RelayTour): Funit =
    coll.updateField($id(tour.id), "syncedAt", nowInstant).void

  def denormalize(
      tourId: RelayTourId,
      active: Boolean,
      live: Boolean,
      dates: Option[RelayTour.Dates]
  ): Funit =
    coll.update.one($id(tourId), $set("active" -> active, "live" -> live, "dates" -> dates)).void

  def oldActiveCursor =
    coll
      .find($doc("active" -> true, "dates.end".$lt(nowInstant.minusDays(1))))
      .cursor[RelayTour]()

  def lookup(local: String) =
    $lookup.simple(
      coll,
      "tour",
      local,
      "_id",
      pipe = List($doc("$project" -> modelProjection))
    )

  def countByOwner(owner: UserId, publicOnly: Boolean): Fu[Int] =
    coll.secondary.countSel(selectors.ownerId(owner) ++ publicOnly.so(selectors.vis.public))

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

  def previews(ids: List[RelayTourId]): Fu[List[TourPreview]] =
    coll.byOrderedIds[TourPreview, RelayTourId](
      ids,
      $doc("name" -> true, "live" -> true, "active" -> true).some
    )(_.id)

  def byIds(ids: List[RelayTourId]): Fu[List[RelayTour]] =
    coll.byOrderedIds[RelayTour, RelayTourId](ids, unsetHeavyOptionalFields.some)(_.id)

  def isOwnerOfAll(u: UserId, ids: List[RelayTourId]): Fu[Boolean] =
    coll.exists($doc($inIds(ids), "ownerIds".$ne(u))).not

  def showTeamScores(id: RelayTourId): Fu[Boolean] =
    coll.primitiveOne[Boolean]($id(id), "showTeamScores").map(~_)

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
      framework.PipelineOperator:
        $lookup.simple(
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
      $doc(
        "dates.start" -> $doc("$lte" -> date.plusMonths(1)),
        $or( // uses 2 index scans then OR on mongodb 7, or one index scan on mongodb 8. Both are ok with current volume
          "dates.end".$gte(date),
          "dates.end".$exists(false)
        )
      )

  private[relay] val modelProjection = $doc(
    "subscribers" -> false,
    "notified" -> false
  )

  private[relay] val unsetHeavyOptionalFields = modelProjection ++ $doc(
    "markup" -> false,
    "players" -> false,
    "teams" -> false
  )

  private[relay] def readTourWithRounds(doc: Bdoc): Option[RelayTour.WithRounds] = for
    tour <- doc.asOpt[RelayTour]
    rounds <- doc.getAsOpt[List[RelayRound]]("rounds")
    if rounds.nonEmpty
  yield tour.withRounds(rounds)

  private[relay] def readToursWithRoundAndGroup[A](
      as: (RelayTour, RelayRound, Option[RelayGroup.Name]) => A
  )(docs: List[Bdoc]): List[A] = for
    doc <- docs
    tour <- doc.asOpt[RelayTour]
    round <- doc.getAsOpt[RelayRound]("round")
    g = group.readFrom(doc)
  yield as(tour, round, g)
