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
          $doc("$match"     -> $doc("finished" -> false)),
          $doc("$addFields" -> $doc("sync.log" -> $arr())),
          $doc("$sort"      -> RelayRoundRepo.sort.chrono),
          $doc("$limit"     -> 1)
        )
      )
      for
        upcoming <- upcoming.get({})
        max = 100
        docs <- colls.tour
          .aggregateList(max): framework =>
            import framework.*
            Match(
              RelayTourRepo.selectors.officialActive ++ $doc("_id".$nin(upcoming.map(_.tour.id)))
            ) -> List(
              Sort(Descending("tier")),
              PipelineOperator(group.lookup(colls.group)),
              Match(group.filter),
              PipelineOperator(roundLookup),
              UnwindField("round"),
              Limit(max)
            )
        tours = for
          doc   <- docs
          tour  <- doc.asOpt[RelayTour]
          round <- doc.getAsOpt[RelayRound]("round")
          group = RelayListing.group.readFrom(doc)
        yield (tour, round, group)
        sorted = tours.sortBy: (tour, round, _) =>
          (
            !round.startedAt.isDefined,                    // ongoing tournaments first
            0 - ~tour.tier,                                // then by tier
            0 - ~round.crowd,                              // then by viewers
            round.startsAt.fold(Long.MaxValue)(_.toMillis) // then by next round date
          )
        active <- sorted.traverse: (tour, round, group) =>
          defaultRoundToShow
            .get(tour.id)
            .map: link =>
              RelayTour.ActiveWithSomeRounds(tour, display = round, link = link | round, group)
      yield
        spotlightCache = active
          .filter(_.tour.spotlight.exists(_.enabled))
          .filterNot(_.display.finished)
          .filter: tr =>
            tr.display.hasStarted || tr.display.startsAt.exists(_.isBefore(nowInstant.plusMinutes(30)))
        active

  val upcoming = cacheApi.unit[List[RelayTour.WithLastRound]]:
    _.refreshAfterWrite(14 seconds).buildAsyncFuture: _ =>
      val max = 64
      colls.tour
        .aggregateList(max): framework =>
          import framework.*
          Match(RelayTourRepo.selectors.officialActive) -> List(
            Sort(Descending("tier")),
            PipelineOperator(group.lookup(colls.group)),
            Match(group.filter),
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
              0 - ~rt.tour.tier,                                // tier sort
              rt.round.startsAt.fold(Long.MaxValue)(_.toMillis) // then by next round date
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
            if next.startsAt.exists(_.isBefore(nowInstant.plusHours(1)))
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
    def lookup(groupColl: Coll) = $lookup.pipelineFull(
      from = groupColl.name,
      as = "group",
      let = $doc("id" -> "$_id"),
      pipe = List(
        $doc("$match" -> $doc("$expr" -> $doc("$in" -> $arr("$$id", "$tours")))),
        $doc:
          "$project" -> $doc(
            "_id"   -> false,
            "name"  -> true,
            "first" -> $doc("$eq" -> $arr("$$id", $doc("$first" -> "$tours")))
          )
      )
    )
    val filter = $doc("group.0.first".$ne(false))

    def readFrom(doc: Bdoc): Option[RelayGroup.Name] = for
      garr <- doc.getAsOpt[Barr]("group")
      gdoc <- garr.getAsOpt[Bdoc](0)
      name <- gdoc.getAsOpt[RelayGroup.Name]("name")
    yield name
