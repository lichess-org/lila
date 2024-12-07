package lila.relay

import reactivemongo.api.bson.*
import monocle.syntax.all.*

import lila.db.dsl.{ *, given }
import lila.relay.RelayTour.{ WithLastRound, ActiveWithSomeRounds }

final class RelayListing2(
    colls: RelayColls,
    tourRepo: RelayTourRepo,
    groupRepo: RelayGroupRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  import RelayListing.*
  import BSONHandlers.{ *, given }

  enum Spot:
    case UngroupedTour(tour: RelayTour.WithRounds)                                    extends Spot
    case GroupWithTours(group: RelayGroup, tours: NonEmptyList[RelayTour.WithRounds]) extends Spot

  def active: Fu[List[RelayTour.ActiveWithSomeRounds]] = getSpots.map:
    _.flatMap:
      case Spot.UngroupedTour(t) =>
        firstUnfinishedRoundOf(t).map: round =>
          ActiveWithSomeRounds(t.tour, round, round, none)
      case Spot.GroupWithTours(group, tours) =>
        tours
          .sortBy(_.tour.tierPriority)
          .foldLeft(none): (found, t) =>
            found.orElse:
              firstUnfinishedRoundOf(t).map: round =>
                ActiveWithSomeRounds(t.tour, round, round, group.name.some)
    .sortBy: t =>
      val datePriority = t.display.startedAt.orElse(t.display.startsAtTime).so(-_.toSeconds)
      (t.tour.tierPriority, datePriority)

  private def firstUnfinishedRoundOf(t: RelayTour.WithRounds): Option[RelayRound] =
    t.rounds.find(!_.isFinished)

  private def getSpots: Fu[List[Spot]] = for
    tours  <- toursWithRounds
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
      .aggregateList(200): framework =>
        import framework.*
        Match(RelayTourRepo.selectors.officialActive) -> List(
          Project($doc("subscribers" -> false, "notified" -> false, "teams" -> false, "players" -> false)),
          Sort(Descending("tier")),
          Limit(max),
          PipelineOperator:
            $lookup.pipelineBC(
              from = colls.round,
              as = "rounds",
              local = "_id",
              foreign = "tourId",
              pipe = List($doc("$sort" -> RelayRoundRepo.sort.asc))
            )
        )
      .map: docs =>
        for
          doc    <- docs
          tour   <- doc.asOpt[RelayTour]
          rounds <- doc.getAsOpt[List[RelayRound]]("rounds")
        yield tour.withRounds(rounds)
