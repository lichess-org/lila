package lila.relay

import reactivemongo.api.bson.*
import monocle.syntax.all.*

import lila.db.dsl.{ *, given }
import lila.relay.RelayTour.{ WithLastRound, ActiveWithSomeRounds }

final class RelayListing(
    colls: RelayColls,
    tourRepo: RelayTourRepo,
    groupRepo: RelayGroupRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import BSONHandlers.{ *, given }

  private var spotlightCache: List[RelayTour.ActiveWithSomeRounds] = Nil

  def spotlight: List[ActiveWithSomeRounds] = spotlightCache

  private enum Spot:
    case UngroupedTour(tour: RelayTour.WithRounds)                                    extends Spot
    case GroupWithTours(group: RelayGroup, tours: NonEmptyList[RelayTour.WithRounds]) extends Spot

  private case class Selected(t: RelayTour.WithRounds, round: RelayRound, group: Option[RelayGroup.Name]):
    export t.tour
  private given Ordering[Selected] = Ordering.by[Selected, (Int, Boolean)]: s =>
    (0 - ~s.tour.tier, !s.round.hasStarted)

  def active: Fu[List[ActiveWithSomeRounds]] = activeCache.get({})

  private val activeCache = cacheApi.unit[List[RelayTour.ActiveWithSomeRounds]]:
    _.refreshAfterWrite(5 seconds).buildAsyncFuture: _ =>
      for
        spots <- getSpots
        selected = spots.flatMap:
          case Spot.UngroupedTour(t) =>
            t.rounds.find(!_.isFinished).map(Selected(t, _, none))
          case Spot.GroupWithTours(group, tours) =>
            val all = for
              tour  <- tours.toList
              round <- tour.rounds
            yield Selected(tour, round, group.name.some)
            all.sorted.headOption
        withLinkRound = selected.map: s =>
          ActiveWithSomeRounds(
            s.tour,
            s.round,
            link = RelayListing.defaultRoundToLink(s.t) | s.round,
            s.group
          )
        sorted = withLinkRound.sortBy: t =>
          val startAt       = t.display.startedAt.orElse(t.display.startsAtTime)
          val crowdRelevant = startAt.exists(_.isBefore(nowInstant.plusHours(1)))
          (
            0 - ~t.tour.tier,                       // by tier
            crowdRelevant.so(0 - ~t.link.crowd),    // then by viewers
            startAt.fold(Long.MaxValue)(_.toMillis) // then by next round date
          )
      yield
        spotlightCache = sorted
          .filter(_.tour.spotlight.exists(_.enabled))
          .filterNot(_.display.isFinished)
          .filter: tr =>
            tr.display.hasStarted || tr.display.startsAtTime.exists(_.isBefore(nowInstant.plusMinutes(30)))
        sorted

  private def decreaseTierIfDistantNextRound(t: RelayTour.WithRounds): Option[RelayTour.WithRounds] = for
    round   <- t.rounds.find(!_.isFinished)
    tier    <- t.tour.tier
    startAt <- round.startedAt.orElse(round.startsAtTime)
    days = scalalib.time.daysBetween(nowInstant.withTimeAtStartOfDay, startAt)
    visualTier <-
      import RelayTour.Tier.*
      if days > 30 then none
      else if tier == BEST && days > 10 then NORMAL.some
      else if tier == BEST && days > 5 then HIGH.some
      else if tier == HIGH && days > 5 then NORMAL.some
      else tier.some
  yield t.focus(_.tour.tier).replace(visualTier.some)

  private def getSpots: Fu[List[Spot]] = for
    rawTours <- toursWithRounds
    tours = rawTours.flatMap(decreaseTierIfDistantNextRound)
    groups <- groupRepo.byTours(tours.map(_.tour.id))
  yield
    val ungroupedTours = tours
      .filter(t => !groups.exists(_.tours.contains(t.tour.id)))
      .map(Spot.UngroupedTour.apply)
    val groupedTours = groups.flatMap: group =>
      tours.filter(t => group.tours.contains(t.tour.id)).toNel.map(Spot.GroupWithTours(group, _))
    ungroupedTours ::: groupedTours

  private def toursWithRounds: Fu[List[RelayTour.WithRounds]] =
    val max = 200
    colls.tour
      .aggregateList(max): framework =>
        import framework.*
        Match(RelayTourRepo.selectors.officialActive) -> List(
          // unset heavy fields that we don't use for listing
          Project($doc("subscribers" -> false, "notified" -> false, "teams" -> false, "players" -> false)),
          Sort(Descending("tier")),
          Limit(max),
          PipelineOperator:
            $lookup.pipelineBC(
              from = colls.round,
              as = "rounds",
              local = "_id",
              foreign = "tourId",
              pipe = List(
                $doc("$match" -> $doc("finishedAt".$exists(false))),
                $doc("$sort"  -> RelayRoundRepo.sort.asc)
              )
            )
        )
      .map: docs =>
        for
          doc    <- docs
          tour   <- doc.asOpt[RelayTour]
          rounds <- doc.getAsOpt[List[RelayRound]]("rounds")
          if rounds.nonEmpty
        yield tour.withRounds(rounds)

  val defaultRoundToLink = cacheApi[RelayTourId, Option[RelayRound]](32, "relay.defaultRoundToLink"):
    _.expireAfterWrite(5 seconds).buildAsyncFuture: tourId =>
      import RelayRoundRepo.sort
      val lastStarted = colls.round
        .find($doc("tourId" -> tourId, "startedAt".$exists(true)))
        .sort($doc("startedAt" -> -1))
        .one[RelayRound]
      val next = colls.round
        .find(RelayRoundRepo.selectors.finished(false) ++ RelayRoundRepo.selectors.tour(tourId))
        .sort(sort.asc)
        .one[RelayRound]
      lastStarted.zip(next).flatMap {
        case (None, _) => // no round started yet, show the first one
          colls.round
            .find($doc("tourId" -> tourId))
            .sort(sort.asc)
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

  // same logic but we have all the rounds in memory already
  def defaultRoundToLink(trs: RelayTour.WithRounds): Option[RelayRound] =
    if !trs.tour.active then trs.rounds.headOption
    else
      trs.rounds
        .flatMap: round =>
          round.startedAt.map(_ -> round)
        .sortBy(-_._1.getEpochSecond)
        .headOption
        .map(_._2)
        .match
          case None => trs.rounds.headOption
          case Some(last) =>
            trs.rounds.find(!_.isFinished) match
              case None => last.some
              case Some(next) =>
                if next.startsAtTime.exists(_.isBefore(nowInstant.plusHours(1)))
                then next.some
                else last.some
