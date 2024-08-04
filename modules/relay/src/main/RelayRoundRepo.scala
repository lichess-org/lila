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
      .sort(sort.chrono)
      .cursor[RelayRound]()

  def byTourOrdered(tourId: RelayTourId): Fu[List[RelayRound]] =
    byTourOrderedCursor(tourId).list(RelayTour.maxRelays)

  def idsByTourOrdered(tour: RelayTour): Fu[List[RelayRoundId]] =
    coll.primitive[RelayRoundId](
      selector = selectors.tour(tour.id),
      sort = sort.chrono,
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
      .sort(sort.reverseChrono)
      .one[RelayRound]

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

  def nextRoundThatStartsAfterThisOneCompletes(round: RelayRound): Fu[Option[RelayRound]] =
    coll
      .find(
        $doc(
          "tourId"   -> round.tourId,
          "finished" -> false,
          "startsAt" -> BSONHandlers.startsAfterPrevious,
          "createdAt".$gt(round.createdAt)
        )
      )
      .sort($doc("createdAt" -> 1))
      .cursor[RelayRound]()
      .uno

private object RelayRoundRepo:

  object sort:
    val chrono        = $doc("createdAt" -> 1)
    val reverseChrono = $doc("createdAt" -> -1)
    val start         = $doc("startedAt" -> -1, "startsAt" -> -1, "name" -> -1)

  object selectors:
    def tour(id: RelayTourId) = $doc("tourId" -> id)
