package lila.relay

import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final private class RelayRoundRepo(val coll: Coll)(using Executor):

  import RelayRoundRepo.*
  import BSONHandlers.given

  def exists(id: RelayRoundId): Fu[Boolean] = coll.exists($id(id))

  def byTourOrderedCursor(tourId: RelayTourId) =
    coll
      .find(selectors.tour(tourId))
      .sort(sort.asc)
      .cursor[RelayRound]()

  def byTourOrdered(tourId: RelayTourId): Fu[List[RelayRound]] =
    byTourOrderedCursor(tourId).list(RelayTour.maxRelays)

  def idsByTourOrdered(tour: RelayTourId): Fu[List[RelayRoundId]] =
    coll.primitive[RelayRoundId](
      selector = selectors.tour(tour),
      sort = sort.asc,
      nb = RelayTour.maxRelays,
      field = "_id"
    )

  def tourIdByStudyId(studyId: StudyId): Fu[Option[RelayTourId]] =
    coll.primitiveOne[RelayTourId]($id(studyId), "tourId")

  def idsByTourId(tourId: RelayTourId): Fu[List[StudyId]] =
    coll
      .find(selectors.tour(tourId))
      .cursor[Bdoc]()
      .list(RelayTour.maxRelays)
      .map(_.flatMap(_.getAsOpt[StudyId]("_id")))

  def lastByTour(tour: RelayTour): Fu[Option[RelayRound]] =
    coll
      .find(selectors.tour(tour.id))
      .sort(sort.desc)
      .one[RelayRound]

  def nextOrderByTour(tourId: RelayTourId): Fu[RelayRound.Order] =
    coll
      .primitiveOne[RelayRound.Order]($doc("tourId" -> tourId), sort.desc, "order")
      .dmap:
        case None        => RelayRound.Order(1)
        case Some(order) => order.map(_ + 1)

  def orderOf(roundId: RelayRoundId): Fu[RelayRound.Order] =
    coll.primitiveOne[RelayRound.Order]($id(roundId), "order").dmap(_ | RelayRound.Order(1))

  def deleteByTour(tour: RelayTour): Funit =
    coll.delete.one(selectors.tour(tour.id)).void

  def studyIdsOf(tourId: RelayTourId): Fu[List[StudyId]] =
    coll.distinctEasy[StudyId, List]("_id", selectors.tour(tourId))

  def syncTargetsOfSource(source: RelayRoundId): Funit =
    coll.update
      .one(
        $doc("sync.until".$exists(true), "sync.upstream.roundIds" -> source),
        $set("sync.nextAt"                                        -> nowInstant)
      )
      .void

  def nextRoundThatStartsAfterThisOneCompletes(round: RelayRound): Fu[Option[RelayRound]] = for
    next <- coll
      .find(
        $doc(
          "tourId" -> round.tourId,
          "startedAt".$exists(false),
          "startsAt" -> BSONHandlers.startsAfterPrevious
        )
      )
      .sort(sort.asc)
      .cursor[RelayRound]()
      .uno
    nextOrder <- next.soFu(n => orderOf(n.id))
    curOrder  <- next.isDefined.soFu(orderOf(round.id))
  yield for
    n  <- next
    no <- nextOrder
    co <- curOrder
    if no == co.map(_ + 1)
  yield n

private object RelayRoundRepo:

  object sort:
    val asc  = $doc("order" -> 1)
    val desc = $doc("order" -> -1)

  object selectors:
    def tour(id: RelayTourId) = $doc("tourId" -> id)
