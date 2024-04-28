package lila.relay

import reactivemongo.akkastream.cursorProducer
import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

final private class RelayRoundRepo(val coll: Coll)(using Executor):

  import RelayRoundRepo.*
  import BSONHandlers.given

  def byTourOrderedCursor(tour: RelayTour) =
    coll
      .find(selectors.tour(tour.id))
      .sort(sort.chrono)
      .cursor[RelayRound]()

  def byTourOrdered(tour: RelayTour): Fu[List[RelayRound]] =
    byTourOrderedCursor(tour).list(RelayTour.maxRelays)

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

private object RelayRoundRepo:

  object sort:
    val chrono        = $doc("createdAt" -> 1)
    val reverseChrono = $doc("createdAt" -> -1)
    val start         = $doc("startedAt" -> -1, "startsAt" -> -1, "name" -> -1)

  object selectors:
    def tour(id: RelayTourId) = $doc("tourId" -> id)
