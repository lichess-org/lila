package lila.relay

import reactivemongo.pekkostream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }
import lila.relay.RelayRound.WithTour

final private class RelayRoundRepo(val coll: Coll, tourRepo: RelayTourRepo)(using Executor):

  import RelayRoundRepo.*
  import BSONHandlers.given

  def exists(id: RelayRoundId): Fu[Boolean] = coll.exists(bid(id))

  def byId(id: RelayRoundId) = coll.byId[RelayRound](id)

  def byIdWithTour(id: RelayRoundId): Fu[Option[WithTour]] =
    coll
      .aggregateOne(): framework =>
        import framework.*
        Match(bid(id)) -> List(
          PipelineOperator(tourRepo.lookupByTourId),
          UnwindField("tour")
        )
      .map(_.flatMap(BSONHandlers.readRoundWithTour))

  def byTourOrderedCursor(tourId: RelayTourId, selector: Bdoc = emptyBdoc) =
    coll
      .find(selectors.tour(tourId) ++ selector)
      .sort(sort.asc)
      .cursor[RelayRound]()

  def byTourOrdered(tourId: RelayTourId, selector: Bdoc = emptyBdoc): Fu[List[RelayRound]] =
    byTourOrderedCursor(tourId, selector).list(RelayTour.maxRelays.value)

  def byToursOrdered(tourIds: Seq[RelayTourId], selector: Bdoc = emptyBdoc): Fu[List[RelayRound]] =
    coll
      .find(bdoc("tourId".in(tourIds)) ++ selector)
      .sort(sort.asc)
      .cursor[RelayRound]()
      .list(RelayTour.maxRelays.value * tourIds.size)

  def idsByTourOrdered(tour: RelayTourId): Fu[List[RelayRoundId]] =
    coll.primitive[RelayRoundId](
      selector = selectors.tour(tour),
      sort = sort.asc,
      field = "_id"
    )

  def studyIdsOf(tourId: RelayTourId): Fu[List[StudyId]] =
    idsByTourOrdered(tourId).map(StudyId.from)

  def tourIdByStudyId(studyId: StudyId): Fu[Option[RelayTourId]] =
    coll.primitiveOne[RelayTourId](bid(studyId), "tourId")

  def lastByTour(tour: RelayTour): Fu[Option[RelayRound]] =
    coll
      .find(selectors.tour(tour.id))
      .sort(sort.desc)
      .one[RelayRound]

  def nextOrderByTour(tourId: RelayTourId): Fu[RelayRound.Order] =
    coll
      .primitiveOne[RelayRound.Order](bdoc("tourId" -> tourId), sort.desc, "order")
      .dmap:
        case None => RelayRound.Order(1)
        case Some(order) => order.map(_ + 1)

  def orderOf(roundId: RelayRoundId): Fu[RelayRound.Order] =
    coll.primitiveOne[RelayRound.Order](bid(roundId), "order").dmap(_ | RelayRound.Order(1))

  def deleteByTour(tour: RelayTour): Funit =
    coll.delete.one(selectors.tour(tour.id)).void

  def syncTargetsOfSource(source: RelayRoundId): Funit =
    coll.update
      .one(
        bdoc("sync.until".exists(true), "sync.upstream.roundIds" -> source),
        set("sync.nextAt" -> nowInstant)
      )
      .void

  def currentCrowd(id: RelayRoundId): Fu[Option[Int]] =
    coll.primitiveOne[Int](bid(id), "crowd")

  def nextRoundThatStartsAfterThisOneCompletes(round: RelayRound): Fu[Option[RelayRound]] = for
    next <- coll
      .find(
        bdoc(
          "tourId" -> round.tourId,
          selectors.started(false),
          "startsAt" -> BSONHandlers.startsAfterPrevious
        )
      )
      .sort(sort.asc)
      .cursor[RelayRound]()
      .uno
    nextOrder <- next.traverse(n => orderOf(n.id))
    curOrder <- next.isDefined.optionFu(orderOf(round.id))
  yield
    for
      n <- next
      no <- nextOrder
      co <- curOrder
      if no == co.map(_ + 1)
    yield n

  private[relay] def reorder(round: RelayRound, moveUp: RelayRoundForm.Move): Funit = {
    for
      roundIds <- idsByTourOrdered(round.tourId)
      newOrder =
        val i = roundIds.indexOf(round.id) - (if moveUp then 1 else 0)
        val arr = roundIds.toArray
        val tmp = arr(i)
        arr(i) = arr(i + 1)
        arr(i + 1) = tmp
        arr.toList
      bulk = coll.update(ordered = false)
      updates <- newOrder.zipWithIndex.parallel: (roundId, i) =>
        bulk.element(q = bid(roundId), u = set("order" -> (i + 1)))
      _ <- bulk.many(updates)
    yield ()
  }.recover:
    case _: ArrayIndexOutOfBoundsException =>

  private[relay] val tourRoundPipeline: Bdoc =
    lookup.simple(
      from = coll,
      as = "rounds",
      local = "_id",
      foreign = "tourId",
      pipe = List(bdoc("$sort" -> RelayRoundRepo.sort.asc))
    )

  private[relay] def isInternalWithoutDelay(id: RelayRoundId): Fu[Boolean] = coll.exists:
    bid(id) ++ selectors.finished(false) ++
      bdoc(
        "sync.delay".exists(false) ++ or(
          bdoc("sync.upstream.ids".exists(true)),
          bdoc("sync.upstream.users".exists(true))
        )
      )

private object RelayRoundRepo:

  object sort:
    val asc = bdoc("order" -> 1)
    val desc = bdoc("order" -> -1)

  object selectors:
    def tour(id: RelayTourId) = bdoc("tourId" -> id)
    def started(v: Boolean) = bdoc("startedAt".exists(v))
    def finished(v: Boolean) = bdoc("finishedAt".exists(v))
    val notLongFinished =
      or(
        bdoc("finishedAt".exists(false)),
        bdoc("finishedAt" -> gt(nowInstant.minusHours(1)))
      )
