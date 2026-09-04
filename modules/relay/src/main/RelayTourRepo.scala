package lila.relay

import reactivemongo.pekkostream.cursorProducer
import java.time.YearMonth

import lila.db.dsl.{ *, given }
import lila.relay.BSONHandlers.given
import lila.core.study.Visibility

final private class RelayTourRepo(val coll: Coll)(using Executor):

  import RelayTourRepo.*
  import RelayTour.TourPreview

  def byId(tourId: RelayTourId): Fu[Option[RelayTour]] = coll.byIdProj[RelayTour](tourId, modelProjection)

  def setSyncedNow(tour: RelayTour): Funit =
    coll.updateField(bid(tour.id), "syncedAt", nowInstant).void

  def denormalize(
      tourId: RelayTourId,
      active: Boolean,
      live: Boolean,
      dates: Option[RelayTour.Dates]
  ): Funit =
    coll.update.one(bid(tourId), $set("active" -> active, "live" -> live, "dates" -> dates)).void

  def oldActiveCursor =
    coll
      .find(bdoc("active" -> true, "dates.end".$lt(nowInstant.minusDays(1))))
      .cursor[RelayTour]()

  def lookup(local: String) =
    $lookup.simple(
      coll,
      "tour",
      local,
      "_id",
      pipe = List(bdoc("$project" -> modelProjection))
    )

  def countByOwner(owner: UserId, publicOnly: Boolean): Fu[Int] =
    coll.secondary.countSel(selectors.ownerId(owner) ++ publicOnly.so(selectors.vis.public))

  def subscribers(tid: RelayTourId): Fu[Set[UserId]] =
    coll.distinctEasy[UserId, Set]("subscribers", bid(tid))

  def setSubscribed(tid: RelayTourId, uid: UserId, isSubscribed: Boolean): Funit =
    coll.update
      .one(bid(tid), if isSubscribed then $addToSet("subscribers" -> uid) else $pull("subscribers" -> uid))
      .void

  def isSubscribed(tid: RelayTourId, uid: UserId): Fu[Boolean] =
    coll.secondary.exists(bdoc(bid(tid), "subscribers" -> uid))

  def countBySubscriberId(uid: UserId): Fu[Int] =
    coll.countSel(selectors.subscriberId(uid))

  private[relay] def hasNotified(rt: RelayRound.WithTour): Fu[Boolean] =
    coll.exists(bdoc(bid(rt.tour.id), "notified" -> rt.round.id))

  def setNotified(rt: RelayRound.WithTour): Funit =
    coll.update.one(bid(rt.tour.id), $addToSet("notified" -> rt.round.id)).void

  def delete(tour: RelayTour): Funit =
    coll.delete.one(bid(tour.id)).void

  def previews(ids: List[RelayTourId]): Fu[List[TourPreview]] =
    coll.byOrderedIds[TourPreview, RelayTourId](
      ids,
      bdoc("name" -> true, "live" -> true, "active" -> true).some
    )(_.id)

  def byIds(ids: List[RelayTourId]): Fu[List[RelayTour]] =
    coll.byOrderedIds[RelayTour, RelayTourId](ids, unsetHeavyOptionalFields.some)(_.id)

  def hasOfficial(ids: List[RelayTourId]): Fu[Boolean] =
    coll.exists(inIds(ids) ++ selectors.official)

  def isOwnerOfAll(u: UserId, ids: List[RelayTourId]): Fu[Boolean] =
    coll.exists(bdoc(inIds(ids), "ownerIds".$ne(u))).not

  def addOwnerToTours(tourIds: List[RelayTourId], userId: UserId): Funit =
    coll.update
      .one(inIds(tourIds), $addToSet("ownerIds" -> userId), multi = true)
      .void

  def showTeamScores(id: RelayTourId): Fu[Boolean] =
    coll.primitiveOne[Boolean](bid(id), "showTeamScores").map(~_)

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
            bdoc("$sort" -> RelayRoundRepo.sort.desc),
            bdoc("$limit" -> 1),
            bdoc("$addFields" -> bdoc("sync.log" -> barr()))
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
      let = bdoc("tourId" -> "$_id"),
      pipe = List(
        bdoc("$match" -> bdoc("$expr" -> bdoc("$in" -> barr("$$tourId", "$tours")))),
        bdoc:
          "$project" -> bdoc(
            "_id" -> false,
            "name" -> true,
            "isFirst" -> bdoc("$eq" -> barr("$$tourId", bdoc("$first" -> "$tours")))
          )
      )
    )
    val firstFilter = bdoc("group.0.isFirst".$ne(false))

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
    val official = bdoc("tier".$exists(true))
    val nonOfficial = bdoc("tier".$exists(false))
    object vis:
      val public = bdoc("visibility" -> Visibility.public)
      val notPublic = bdoc("visibility".$ne(Visibility.public))
      val `private` = bdoc("visibility" -> Visibility.`private`)
    val officialPublic = official ++ vis.public
    val officialNotPublic = official ++ vis.notPublic
    val active = bdoc("active" -> true)
    val inactive = bdoc("active" -> false)
    def ownerId(u: UserId) = bdoc("ownerIds" -> u)
    def subscriberId(u: UserId) = bdoc("subscribers" -> u)
    val officialActive = officialPublic ++ active
    val officialInactive = officialPublic ++ inactive
    val live = bdoc("live" -> true)
    def inMonth(at: YearMonth) =
      val date = java.time.LocalDate.of(at.getYear, at.getMonth, 1)
      bdoc(
        "dates.start" -> bdoc("$lte" -> date.plusMonths(1)),
        $or( // uses 2 index scans then OR on mongodb 7, or one index scan on mongodb 8. Both are ok with current volume
          "dates.end".$gte(date),
          "dates.end".$exists(false)
        )
      )

  private[relay] val modelProjection = bdoc(
    "subscribers" -> false,
    "notified" -> false
  )

  private[relay] val unsetHeavyOptionalFields = modelProjection ++ bdoc(
    "markup" -> false,
    "players" -> false,
    "teams" -> false
  )

  private[relay] def readTourWithRounds(doc: Bdoc): Option[RelayTour.WithRounds] = for
    tour <- doc.asOpt[RelayTour]
    rounds <- doc.getAsOpt[List[RelayRound]]("rounds")
    if rounds.nonEmpty
  yield tour.withRounds(rounds)

  private[relay] def readTourWithRoundsAndGroup(
      doc: Bdoc
  ): Option[(RelayTour.WithRounds, Option[RelayGroup.Name])] = for
    tour <- readTourWithRounds(doc)
    group = RelayTourRepo.group.readFrom(doc)
  yield tour -> group

  private[relay] def readToursWithRoundAndGroup[A](
      as: (RelayTour, RelayRound, Option[RelayGroup.Name]) => A
  )(docs: List[Bdoc]): List[A] = for
    doc <- docs
    tour <- doc.asOpt[RelayTour]
    round <- doc.getAsOpt[RelayRound]("round")
    g = group.readFrom(doc)
  yield as(tour, round, g)
