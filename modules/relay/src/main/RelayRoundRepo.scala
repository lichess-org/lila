package lila.relay

import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.relay.RelayRound.WithTour

final private class RelayRoundRepo(val coll: Coll, tourRepo: RelayTourRepo)(using Executor):

  import RelayRoundRepo.*
  import BSONHandlers.given

  def exists(id: RelayRoundId): Fu[Boolean] = coll.exists($id(id))

  def byId(id: RelayRoundId) = coll.byId[RelayRound](id)

  def byIdWithTour(id: RelayRoundId): Fu[Option[WithTour]] =
    coll
      .aggregateOne(): framework =>
        import framework.*
        Match($id(id)) -> List(
          PipelineOperator(tourRepo.lookup("tourId")),
          UnwindField("tour")
        )
      .map(_.flatMap(BSONHandlers.readRoundWithTour))

  def byTourOrderedCursor(tourId: RelayTourId) =
    coll
      .find(selectors.tour(tourId))
      .sort(sort.asc)
      .cursor[RelayRound]()

  def byTourOrdered(tourId: RelayTourId): Fu[List[RelayRound]] =
    byTourOrderedCursor(tourId).list(RelayTour.maxRelays.value)

  def idsByTourOrdered(tour: RelayTourId): Fu[List[RelayRoundId]] =
    coll.primitive[RelayRoundId](
      selector = selectors.tour(tour),
      sort = sort.asc,
      field = "_id"
    )

  def studyIdsOf(tourId: RelayTourId): Fu[List[StudyId]] =
    idsByTourOrdered(tourId).map(StudyId.from)

  def tourIdByStudyId(studyId: StudyId): Fu[Option[RelayTourId]] =
    coll.primitiveOne[RelayTourId]($id(studyId), "tourId")

  def lastByTour(tour: RelayTour): Fu[Option[RelayRound]] =
    coll
      .find(selectors.tour(tour.id))
      .sort(sort.desc)
      .one[RelayRound]

  def nextOrderByTour(tourId: RelayTourId): Fu[RelayRound.Order] =
    coll
      .primitiveOne[RelayRound.Order]($doc("tourId" -> tourId), sort.desc, "order")
      .dmap:
        case None => RelayRound.Order(1)
        case Some(order) => order.map(_ + 1)

  def orderOf(roundId: RelayRoundId): Fu[RelayRound.Order] =
    coll.primitiveOne[RelayRound.Order]($id(roundId), "order").dmap(_ | RelayRound.Order(1))

  def deleteByTour(tour: RelayTour): Funit =
    coll.delete.one(selectors.tour(tour.id)).void

  def syncTargetsOfSource(source: RelayRoundId): Funit =
    coll.update
      .one(
        $doc("sync.until".$exists(true), "sync.upstream.roundIds" -> source),
        $set("sync.nextAt" -> nowInstant)
      )
      .void

  def currentCrowd(id: RelayRoundId): Fu[Option[Int]] =
    coll.primitiveOne[Int]($id(id), "crowd")

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
    nextOrder <- next.traverse(n => orderOf(n.id))
    curOrder <- next.isDefined.optionFu(orderOf(round.id))
  yield for
    n <- next
    no <- nextOrder
    co <- curOrder
    if no == co.map(_ + 1)
  yield n

private object RelayRoundRepo:

  object sort:
    val asc = $doc("order" -> 1)
    val desc = $doc("order" -> -1)

  object selectors:
    def tour(id: RelayTourId) = $doc("tourId" -> id)
    def finished(v: Boolean) = $doc("finishedAt".$exists(v))
