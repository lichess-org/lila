package lila.relay

import reactivemongo.api.bson.*
import reactivemongo.akkastream.cursorProducer

import lila.db.dsl.{ *, given }

final private class RelayRoundRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  def byTourOrdered(tour: RelayTour): Fu[List[RelayRound]] =
    coll
      .find(selectors.tour(tour.id))
      .sort(sort.chrono)
      .cursor[RelayRound]()
      .list(RelayTour.maxRelays)

  def idsByTourOrdered(tour: RelayTour): Fu[List[RelayRoundId]] =
    coll
      .find(selectors.tour(tour.id), $id(true).some)
      .sort(sort.chrono)
      .cursor[Bdoc]()
      .list(RelayTour.maxRelays)
      .map(_.flatMap(_.getAsOpt[RelayRoundId]("_id")))

  def lastByTour(tour: RelayTour): Fu[Option[RelayRound]] =
    coll
      .find(selectors tour tour.id)
      .sort(sort.reverseChrono)
      .one[RelayRound]

  private[relay] object sort:
    val chrono        = $doc("createdAt" -> 1)
    val reverseChrono = $doc("createdAt" -> -1)
    val start         = $doc("startedAt" -> -1, "startsAt" -> -1, "name" -> -1)

  private[relay] object selectors:
    def tour(id: RelayTour.Id) = $doc("tourId" -> id)
