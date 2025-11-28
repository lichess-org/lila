package lila.relay

import monocle.syntax.all.*
import java.time.temporal.ChronoUnit
import lila.memo.CacheApi.buildAsyncTimeout

import lila.db.dsl.*

private final class RelayListing(
    groupRepo: RelayGroupRepo,
    tourRepo: RelayTourRepo,
    roundRepo: RelayRoundRepo,
    groupCrowd: RelayGroupCrowdSumCache,
    cacheApi: lila.memo.CacheApi
)(using Executor, Scheduler):

  import RelayTour.tierPriority

  def active: Fu[List[RelayCard]] = activeCache.get({}).recoverDefault

  val activeCacheTtl = 5.seconds

  private enum Spot:
    case UngroupedTour(tour: RelayTour.WithRounds) extends Spot
    case GroupWithTours(group: RelayGroup, tours: NonEmptyList[RelayTour.WithRounds]) extends Spot

  private case class Selected(t: RelayTour.WithRounds, round: RelayRound, group: Option[RelayGroup.Name])

  private val activeCache = cacheApi.unit[List[RelayCard]]:
    _.expireAfterWrite(activeCacheTtl).buildAsyncTimeout(): _ =>
      for
        spots <- getSpots
        selected = spots.flatMap:
          case Spot.UngroupedTour(t) =>
            t.rounds.find(!_.isFinished).map(Selected(t, _, none)).map(NonEmptyList.one)
          case Spot.GroupWithTours(group, tours) =>
            val all = for
              tour <- tours.toList
              round <- tour.rounds.find(!_.isFinished)
            yield Selected(tour, round, group.name.some)
            // sorted preserves the original ordering while adding its own
            all.sorted(using Ordering.by(s => (!s.round.hasStarted, tierPriority(s.t.tour)))).take(3).toNel
        cards <- selected.traverse(toRelayCard)
        sorted = cards.sortBy: t =>
          val startAt = t.display.startedAt.orElse(t.display.startsAtTime)
          val crowdRelevant = startAt.exists(_.isBefore(nowInstant.plusHours(1)))
          (
            tierPriority(t.tour), // by tier
            crowdRelevant.so(0 - t.crowd.value), // then by viewers
            startAt.fold(Long.MaxValue)(_.toMillis) // then by next round date
          )
      yield sorted

  private def toRelayCard(s: NonEmptyList[Selected]): Fu[RelayCard] =
    val main = s.head
    groupCrowd
      .get(main.t.tour.id)
      .map: crowd =>
        RelayCard(
          tour = main.t.tour,
          display = main.round,
          link = RelayDefaults.defaultRoundToLink(main.t) | main.round,
          crowd = crowd,
          group = main.group,
          alts = s.tail.filter(_.round.hasStarted).map(s => s.round.withTour(s.t.tour))
        )

  private object dynamicTier:

    def apply(t: RelayTour.WithRounds): Option[RelayTour.WithRounds] =
      nextRoundTier(t)
        .orElse(lastRoundTier(t))
        .map: tier =>
          t.focus(_.tour.tier).replace(tier.some)

    private def nextRoundTier(t: RelayTour.WithRounds): Option[RelayTour.Tier] = for
      round <- t.rounds.find(!_.isFinished)
      tier <- t.tour.tier
      startAt <- round.startedAt
        .orElse(round.startsAtTime)
        .orElse:
          round.startsAfterPrevious.option(nowInstant)
      days = scalalib.time.daysBetween(nowInstant.withTimeAtStartOfDay, startAt)
      newTier <-
        import RelayTour.Tier.*
        if days > 30 then none
        else if tier == best && days > 10 then normal.some
        else if tier == best && days > 5 then high.some
        else if tier == high && days > 5 then normal.some
        else tier.some
    yield newTier

    private def lastRoundTier(t: RelayTour.WithRounds): Option[RelayTour.Tier] = for
      round <- t.rounds.findLast(_.isFinished)
      tier <- t.tour.tier
      finishAt <- round.finishedAt
      hours = ChronoUnit.HOURS.between(finishAt, nowInstant).toInt
      newTier <-
        import RelayTour.Tier.*
        if hours > 48 then none
        else if tier == best then
          if hours < 6 then best.some
          else if hours < 24 then high.some
          else normal.some
        else if tier == high then
          if hours < 3 then high.some
          else if hours < 24 then normal.some
          else none
        else if hours < 3 then normal.some
        else none
    yield newTier

  private def getSpots: Fu[List[Spot]] = for
    rawTours <- toursWithRounds
    tours = rawTours.flatMap(dynamicTier.apply)
    groups <- groupRepo.byTours(tours.map(_.tour.id))
  yield
    val toursById = tours.mapBy(_.tour.id)
    val ungroupedTours: List[Spot] = tours
      .filter(t => !groups.exists(_.tours.contains(t.tour.id)))
      .map(Spot.UngroupedTour.apply)
    val groupedTours: List[Spot] = groups.flatMap: group =>
      group.tours.flatMap(toursById.get).toNel.map(Spot.GroupWithTours(group, _))
    ungroupedTours ::: groupedTours

  private def toursWithRounds: Fu[List[RelayTour.WithRounds]] =
    val max = 200
    tourRepo.coll
      .aggregateList(max): framework =>
        import framework.*
        Match(RelayTourRepo.selectors.officialActive) -> List(
          Project(RelayTourRepo.unsetHeavyOptionalFields),
          Sort(Descending("tier")),
          Limit(max),
          PipelineOperator(roundRepo.tourRoundPipeline)
        )
      .map(_.flatMap(RelayTourRepo.readTourWithRounds))
