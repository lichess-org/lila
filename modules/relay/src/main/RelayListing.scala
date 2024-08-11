package lila.relay

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.relay.RelayTour.ActiveWithSomeRounds

final class RelayListing(
    colls: RelayColls,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import RelayListing.*
  import BSONHandlers.{ given }

  private var spotlightCache: List[RelayTour.ActiveWithSomeRounds] = Nil

  def spotlight: List[ActiveWithSomeRounds] = spotlightCache

  val active = cacheApi.unit[List[RelayTour.ActiveWithSomeRounds]]:
    _.refreshAfterWrite(5 seconds).buildAsyncFuture: _ =>

      val roundLookup = $lookup.pipeline(
        from = colls.round,
        as = "round",
        local = "_id",
        foreign = "tourId",
        pipe = List(
          $doc("$match" -> $doc("finished" -> false)),
          $doc("$sort"  -> RelayRoundRepo.sort.chrono),
          $doc("$limit" -> 1)
        )
      )
      for
        upcoming <- upcoming.get({})
        max = 100
        tourIds <- colls.tour.distinctEasy[RelayTourId, List](
          "_id",
          RelayTourRepo.selectors.officialActive ++ $doc("_id".$nin(upcoming.map(_.tour.id)))
        )
        groupToursDocs <- colls.group.aggregateList(Int.MaxValue): framework =>
          import framework.*
          Match($doc("tours".$in(tourIds))) -> List(
            PipelineOperator(
              $lookup.pipelineFull(
                from = colls.tour.name,
                as = "tours",
                let = $doc("tourIds" -> "$tours"),
                pipe = List(
                  $doc("$match"     -> $doc("$expr" -> $doc("$in" -> $arr("$_id", "$$tourIds")))),
                  $doc("$addFields" -> $doc("__order" -> $doc("$indexOfArray" -> $arr("$$tourIds", "$_id")))),
                  $doc("$sort"      -> $doc("tier" -> -1, "__order" -> 1)),
                  $doc("$project"   -> $doc("live" -> true))
                )
              )
            ),
            Project(
              $doc(
                "tours" -> $doc(
                  "$ifNull" -> $arr(
                    $doc(
                      "$first" -> $doc(
                        "$filter" -> $doc(
                          "input" -> "$tours",
                          "as"    -> "tour",
                          "cond"  -> "$$tour.live"
                          // "limit" -> 1 // TODO unsupported by mongodb 4.4 (but also not needed here)
                        )
                      )
                    ),
                    $doc("$first" -> "$tours")
                  )
                )
              )
            ),
            Project($doc("_id" -> true, "tour" -> "$tours._id"))
          )
        groupTourPairs = for
          doc     <- groupToursDocs
          groupId <- doc.getAsOpt[RelayGroup.Id]("_id")
          tour    <- doc.getAsOpt[RelayTourId]("tour")
        yield s"$groupId$tour"
        docs <- colls.tour
          .aggregateList(max): framework =>
            import framework.*
            Match($inIds(tourIds)) -> List(
              Sort(Descending("tier")),
              Project(
                $doc("subscribers" -> false, "notified" -> false, "teams" -> false, "players" -> false)
              ),
              PipelineOperator(
                $lookup.pipelineFull(
                  from = colls.group.name,
                  as = "group",
                  let = $doc("tourId" -> "$_id"),
                  pipe = List(
                    $doc("$match" -> $doc("$expr" -> $doc("$in" -> $arr("$$tourId", "$tours")))),
                    $doc(
                      "$project" -> $doc(
                        "_id"  -> true,
                        "name" -> true
                      )
                    )
                  )
                )
              ),
              AddFields($doc("group" -> $doc("$first" -> "$group"))),
              AddFields(
                $doc(
                  "isGroupTour" ->
                    $doc(
                      "$let" -> $doc(
                        "vars" -> $doc(
                          "allPairs" -> groupTourPairs,
                          "pair"     -> $doc("$concat" -> $arr("$group._id", "$_id"))
                        ),
                        "in" -> $doc("$in" -> $arr("$$pair", "$$allPairs"))
                      )
                    )
                )
              ),
              Match($doc($or("group".$exists(false), "isGroupTour".$eq(true)))),
              PipelineOperator(roundLookup),
              UnwindField("round"),
              Limit(max)
            )
        tours = for
          doc   <- docs
          tour  <- doc.asOpt[RelayTour]
          round <- doc.getAsOpt[RelayRound]("round")
          group = RelayListing.group.readFromOne(doc)
        yield (tour, round, group)
        sorted = tours.sortBy: (tour, round, _) =>
          (
            !round.hasStarted,                                 // ongoing tournaments first
            0 - ~tour.tier,                                    // then by tier
            0 - ~round.crowd,                                  // then by viewers
            round.startsAtTime.fold(Long.MaxValue)(_.toMillis) // then by next round date
          )
        active <- sorted.parallel: (tour, round, group) =>
          defaultRoundToShow
            .get(tour.id)
            .map: link =>
              RelayTour.ActiveWithSomeRounds(tour, display = round, link = link | round, group)
      yield
        spotlightCache = active
          .filter(_.tour.spotlight.exists(_.enabled))
          .filterNot(_.display.finished)
          .filter: tr =>
            tr.display.hasStarted || tr.display.startsAtTime.exists(_.isBefore(nowInstant.plusMinutes(30)))
        active

  val upcoming = cacheApi.unit[List[RelayTour.WithLastRound]]:
    _.refreshAfterWrite(14 seconds).buildAsyncFuture: _ =>
      val max = 64
      colls.tour
        .aggregateList(max): framework =>
          import framework.*
          Match(RelayTourRepo.selectors.officialActive) -> List(
            Sort(Descending("tier")),
            PipelineOperator(group.firstLookup(colls.group)),
            Match(group.firstFilter),
            PipelineOperator:
              $lookup.pipeline(
                from = colls.round,
                as = "round",
                local = "_id",
                foreign = "tourId",
                pipe = List(
                  $doc("$sort"  -> $sort.asc("startsAt")),
                  $doc("$limit" -> 1),
                  $doc("$match" -> $doc("finished" -> false, "startsAt".$gte(nowInstant)))
                )
              )
            ,
            UnwindField("round"),
            Limit(max)
          )
        .map: docs =>
          for
            doc   <- docs
            tour  <- doc.asOpt[RelayTour]
            round <- doc.getAsOpt[RelayRound]("round")
            group = RelayListing.group.readFrom(doc)
          yield RelayTour.WithLastRound(tour, round, group)
        .map:
          _.sortBy: rt =>
            (
              0 - ~rt.tour.tier,                                    // tier sort
              rt.round.startsAtTime.fold(Long.MaxValue)(_.toMillis) // then by next round date
            )

  val defaultRoundToShow = cacheApi[RelayTourId, Option[RelayRound]](32, "relay.lastAndNextRounds"):
    _.expireAfterWrite(5 seconds).buildAsyncFuture: tourId =>
      val chronoSort = $doc("startsAt" -> 1, "createdAt" -> 1)
      val lastStarted = colls.round
        .find($doc("tourId" -> tourId, "startedAt".$exists(true)))
        .sort($doc("startedAt" -> -1))
        .one[RelayRound]
      val next = colls.round
        .find($doc("tourId" -> tourId, "finished" -> false))
        .sort(chronoSort)
        .one[RelayRound]
      lastStarted.zip(next).flatMap {
        case (None, _) => // no round started yet, show the first one
          colls.round
            .find($doc("tourId" -> tourId))
            .sort(chronoSort)
            .one[RelayRound]
        case (Some(last), Some(next)) => // show the next one if it's less than an hour away
          fuccess:
            if next.startsAtTime.exists(_.isBefore(nowInstant.plusHours(1)))
            then next.some
            else last.some
        case (Some(last), None) =>
          fuccess(last.some)
      }

private object RelayListing:

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
            "_id"     -> false,
            "name"    -> true,
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
