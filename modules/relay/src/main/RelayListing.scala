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

  def spotlight: List[ActiveWithSomeRounds] = spotlightCache

  val defaultRoundToLink = cacheApi[RelayTourId, Option[RelayRound]](32, "relay.defaultRoundToLink"):
    _.expireAfterWrite(5 seconds).buildAsyncFuture: tourId =>
      tourWithUnfinishedRounds(tourId).mapz(RelayListing.defaultRoundToLink)

  def active: Fu[List[ActiveWithSomeRounds]] = activeCache.get({})

  private enum Spot:
    case UngroupedTour(tour: RelayTour.WithRounds)                                    extends Spot
    case GroupWithTours(group: RelayGroup, tours: NonEmptyList[RelayTour.WithRounds]) extends Spot

  private case class Selected(t: RelayTour.WithRounds, round: RelayRound, group: Option[RelayGroup.Name])

  private var spotlightCache: List[RelayTour.ActiveWithSomeRounds] = Nil

  private val activeCache = cacheApi.unit[List[RelayTour.ActiveWithSomeRounds]]:
    _.expireAfterWrite(5 seconds).buildAsyncFuture: _ =>
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
            // sorted preserves the original ordering while adding its own
            all.sorted(using Ordering.by(s => (tierPriority(s.t.tour), !s.round.hasStarted))).headOption
        withLinkRound = selected.map: s =>
          ActiveWithSomeRounds(
            s.t.tour,
            s.round,
            link = RelayListing.defaultRoundToLink(s.t) | s.round,
            s.group
          )
        sorted = withLinkRound.sortBy: t =>
          val startAt       = t.display.startedAt.orElse(t.display.startsAtTime)
          val crowdRelevant = startAt.exists(_.isBefore(nowInstant.plusHours(1)))
          (
            tierPriority(t.tour),                   // by tier
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

  private def tierPriority(t: RelayTour) = -t.tier.so(_.v)

  private def decreaseTierIfDistantNextRound(t: RelayTour.WithRounds): Option[RelayTour.WithRounds] = for
    round   <- t.rounds.find(!_.isFinished)
    tier    <- t.tour.tier
    startAt <- round.startedAt.orElse(round.startsAtTime)
    days = scalalib.time.daysBetween(nowInstant.withTimeAtStartOfDay, startAt)
    visualTier <-
      import RelayTour.Tier.*
      if days > 30 then none
      else if tier == best && days > 10 then normal.some
      else if tier == best && days > 5 then high.some
      else if tier == high && days > 5 then normal.some
      else tier.some
  yield t.focus(_.tour.tier).replace(visualTier.some)

  private def getSpots: Fu[List[Spot]] = for
    rawTours <- toursWithUnfinishedRounds
    tours = rawTours.flatMap(decreaseTierIfDistantNextRound)
    groups <- groupRepo.byTours(tours.map(_.tour.id))
  yield
    val ungroupedTours = tours
      .filter(t => !groups.exists(_.tours.contains(t.tour.id)))
      .map(Spot.UngroupedTour.apply)
    val groupedTours = groups.flatMap: group =>
      tours.filter(t => group.tours.contains(t.tour.id)).toNel.map(Spot.GroupWithTours(group, _))
    ungroupedTours ::: groupedTours

  private def toursWithUnfinishedRounds: Fu[List[RelayTour.WithRounds]] =
    val max = 200
    colls.tour
      .aggregateList(max): framework =>
        import framework.*
        Match(RelayTourRepo.selectors.officialActive) -> List(
          Project(tourUnsets),
          Sort(Descending("tier")),
          Limit(max),
          PipelineOperator(tourRoundPipeline)
        )
      .map(_.flatMap(readTourRound))

  private def tourWithUnfinishedRounds(id: RelayTourId): Fu[Option[RelayTour.WithRounds]] =
    colls.tour
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          Project(tourUnsets),
          PipelineOperator(tourRoundPipeline)
        )
      .map(_.flatMap(readTourRound))

  // unset heavy fields that we don't use for listing
  private val tourUnsets =
    $doc("subscribers" -> false, "notified" -> false, "teams" -> false, "players" -> false)

  private val tourRoundPipeline: Bdoc =
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

  private def readTourRound(doc: Bdoc): Option[RelayTour.WithRounds] = for
    tour   <- doc.asOpt[RelayTour]
    rounds <- doc.getAsOpt[List[RelayRound]]("rounds")
    if rounds.nonEmpty
  yield tour.withRounds(rounds)

private object RelayListing:

  def defaultRoundToLink(trs: RelayTour.WithRounds): Option[RelayRound] =
    if !trs.tour.active then trs.rounds.headOption
    else
      trs.rounds
        .flatMap: round =>
          round.startedAt.map(_ -> round)
        .sortBy(-_._1.getEpochSecond)
        .headOption
        .match
          case None => trs.rounds.headOption
          case Some((_, last)) =>
            trs.rounds.find(!_.isFinished) match
              case None => last.some
              case Some(next) =>
                if next.startsAtTime.exists(_.isBefore(nowInstant.plusHours(1)))
                then next.some
                else last.some
