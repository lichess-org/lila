package lila.relay

import reactivemongo.api.bson.*
import monocle.syntax.all.*
import java.time.temporal.ChronoUnit

import lila.db.dsl.{ *, given }

final class RelayListing(
    colls: RelayColls,
    tourRepo: RelayTourRepo,
    groupRepo: RelayGroupRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import BSONHandlers.{ *, given }

  def spotlight: List[RelayCard] = spotlightCache

  val defaultRoundToLink = cacheApi[RelayTourId, Option[RelayRound]](32, "relay.defaultRoundToLink"):
    _.expireAfterWrite(5.seconds).buildAsyncFuture: tourId =>
      tourWithRounds(tourId).mapz(RelayListing.defaultRoundToLink)

  def active: Fu[List[RelayCard]] = activeCache.get({})

  private enum Spot:
    case UngroupedTour(tour: RelayTour.WithRounds)                                    extends Spot
    case GroupWithTours(group: RelayGroup, tours: NonEmptyList[RelayTour.WithRounds]) extends Spot

  private case class Selected(t: RelayTour.WithRounds, round: RelayRound, group: Option[RelayGroup.Name])

  private var spotlightCache: List[RelayCard] = Nil

  private val activeCache = cacheApi.unit[List[RelayCard]]:
    _.expireAfterWrite(5.seconds).buildAsyncFuture: _ =>
      for
        spots <- getSpots
        selected = spots.flatMap:
          case Spot.UngroupedTour(t) =>
            t.rounds.find(!_.isFinished).map(Selected(t, _, none)).map(NonEmptyList.one)
          case Spot.GroupWithTours(group, tours) =>
            val all = for
              tour  <- tours.toList
              round <- tour.rounds.find(!_.isFinished)
            yield Selected(tour, round, group.name.some)
            // sorted preserves the original ordering while adding its own
            all.sorted(using Ordering.by(s => (tierPriority(s.t.tour), !s.round.hasStarted))).take(3).toNel
        cards = selected.map(toRelayCard)
        sorted = cards.sortBy: t =>
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
          .filter: tr =>
            tr.display.hasStarted || tr.display.startsAtTime.exists(_.isBefore(nowInstant.plusMinutes(30)))
        sorted

  private def toRelayCard(s: NonEmptyList[Selected]): RelayCard =
    val main = s.head
    RelayCard(
      tour = main.t.tour,
      display = main.round,
      link = RelayListing.defaultRoundToLink(main.t) | main.round,
      group = main.group,
      alts = s.tail.filter(_.round.hasStarted).map(s => s.round.withTour(s.t.tour))
    )

  private def tierPriority(t: RelayTour) = -t.tier.so(_.v)

  private object dynamicTier:

    def apply(t: RelayTour.WithRounds): Option[RelayTour.WithRounds] =
      nextRoundTier(t)
        .orElse(lastRoundTier(t))
        .map: tier =>
          t.focus(_.tour.tier).replace(tier.some)

    private def nextRoundTier(t: RelayTour.WithRounds): Option[RelayTour.Tier] = for
      round   <- t.rounds.find(!_.isFinished)
      tier    <- t.tour.tier
      startAt <- round.startedAt.orElse(round.startsAtTime)
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
      round    <- t.rounds.findLast(_.isFinished)
      tier     <- t.tour.tier
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

  private def tourWithRounds(id: RelayTourId): Fu[Option[RelayTour.WithRounds]] =
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
      pipe = List($doc("$sort" -> RelayRoundRepo.sort.asc))
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
          case Some(_, last) =>
            trs.rounds.find(!_.isFinished) match
              case None => last.some
              case Some(next) =>
                if next.startsAtTime.exists(_.isBefore(nowInstant.plusHours(1)))
                then next.some
                else last.some
