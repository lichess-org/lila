package lila.tournament

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONArray }
import reactivemongo.core.commands.Count

import BSONHandlers._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._

object TournamentRepo {

  private lazy val coll = Env.current.tournamentColl

  private def selectId(id: String) = BSONDocument("_id" -> id)

  private val enterableSelect = BSONDocument(
    "status" -> BSONDocument("$in" -> List(Status.Created.id, Status.Started.id)))

  private val createdSelect = BSONDocument("status" -> Status.Created.id)
  private val startedSelect = BSONDocument("status" -> Status.Started.id)
  private val finishedSelect = BSONDocument("status" -> Status.Finished.id)

  def byId(id: String): Fu[Option[Tournament]] = coll.find(selectId(id)).one[Tournament]

  def byIdAndPlayerId(id: String, userId: String): Fu[Option[Tournament]] =
    coll.find(
      selectId(id) ++ BSONDocument("players.id" -> userId)
    ).one[Tournament]

  def createdById(id: String): Fu[Option[Tournament]] =
    coll.find(selectId(id) ++ createdSelect).one[Tournament]

  def enterableById(id: String): Fu[Option[Tournament]] =
    coll.find(selectId(id) ++ enterableSelect).one[Tournament]

  def startedById(id: String): Fu[Option[Tournament]] =
    coll.find(selectId(id) ++ startedSelect).one[Tournament]

  def finishedById(id: String): Fu[Option[Tournament]] =
    coll.find(selectId(id) ++ finishedSelect).one[Tournament]

  def startedOrFinishedById(id: String): Fu[Option[Tournament]] =
    byId(id) map { _ filterNot (_.isCreated) }

  def createdByIdAndCreator(id: String, userId: String): Fu[Option[Tournament]] =
    createdById(id) map (_ filter (_.createdBy == userId))

  def allEnterable: Fu[List[Tournament]] = coll.find(enterableSelect).cursor[Tournament].collect[List]()

  def created: Fu[List[Tournament]] = coll.find(createdSelect ++ BSONDocument(
    "schedule" -> BSONDocument("$exists" -> false)
  )).toList[Tournament](None)

  def createdIncludingScheduled: Fu[List[Tournament]] = coll.find(createdSelect).toList[Tournament](None)

  def started: Fu[List[Tournament]] =
    coll.find(startedSelect).sort(BSONDocument("createdAt" -> -1)).toList[Tournament](None)

  def publicStarted: Fu[List[Tournament]] =
    coll.find(startedSelect ++ BSONDocument("private" -> BSONDocument("$exists" -> false)))
      .sort(BSONDocument("createdAt" -> -1))
      .cursor[Tournament].collect[List]()

  def finished(limit: Int): Fu[List[Tournament]] =
    coll.find(finishedSelect)
      .sort(BSONDocument("startsAt" -> -1))
      .cursor[Tournament].collect[List](limit)

  def finishedNotable(limit: Int): Fu[List[Tournament]] =
    coll.find(finishedSelect ++ BSONDocument(
      "nbPlayers" -> BSONDocument("$gte" -> 15)))
      .sort(BSONDocument("startsAt" -> -1))
      .cursor[Tournament].collect[List](limit)

  def setStatus(tourId: String, status: Status) = coll.update(
    selectId(tourId),
    BSONDocument("$set" -> BSONDocument("status" -> status.id))
  ).void

  def setNbPlayers(tourId: String, nb: Int) = coll.update(
    selectId(tourId),
    BSONDocument("$set" -> BSONDocument("nbPlayers" -> nb))
  ).void

  def setWinnerId(tourId: String, userId: String) = coll.update(
    selectId(tourId),
    BSONDocument("$set" -> BSONDocument("winner" -> userId))
  ).void

  private def allCreatedSelect(aheadMinutes: Int) = createdSelect ++ BSONDocument(
    "$or" -> BSONArray(
      BSONDocument("schedule" -> BSONDocument("$exists" -> false)),
      BSONDocument("startsAt" -> BSONDocument("$lt" -> (DateTime.now plusMinutes aheadMinutes)))
    )
  )

  def publicCreatedSorted(aheadMinutes: Int): Fu[List[Tournament]] = coll.find(
    allCreatedSelect(aheadMinutes) ++ BSONDocument("private" -> BSONDocument("$exists" -> false))
  ).sort(BSONDocument("startsAt" -> 1)).cursor[Tournament].collect[List]()

  def allCreated(aheadMinutes: Int): Fu[List[Tournament]] =
    coll.find(allCreatedSelect(aheadMinutes)).cursor[Tournament].collect[List]()

  private def notCloseToFinishSorted: Fu[List[Tournament]] = {
    val finishAfter = DateTime.now plusMinutes 20
    coll.find(startedSelect ++ BSONDocument(
      "private" -> BSONDocument("$exists" -> false)
    )).sort(BSONDocument("startsAt" -> 1)).toList[Tournament](none) map {
      _.filter(_.finishesAt isAfter finishAfter)
    }
  }

  def promotable: Fu[List[Tournament]] =
    publicCreatedSorted(30) zip notCloseToFinishSorted map {
      case (created, started) => created ::: started
    }

  def scheduled: Fu[List[Tournament]] = coll.find(createdSelect ++ BSONDocument(
    "schedule" -> BSONDocument("$exists" -> true)
  )).sort(BSONDocument("startsAt" -> 1)).cursor[Tournament].collect[List]()

  def scheduledDedup: Fu[List[Tournament]] = scheduled map {
    import Schedule.Freq
    _.flatMap { tour =>
      tour.schedule map (tour -> _)
    }.foldLeft(List[Tournament]() -> none[Freq]) {
      case ((tours, skip), (_, sched)) if skip.contains(sched.freq) => (tours, skip)
      case ((tours, skip), (tour, sched)) => (tour :: tours, sched.freq match {
        case Freq.Daily   => Freq.Nightly.some
        case Freq.Nightly => Freq.Daily.some
        case _            => skip
      })
    }._1.reverse
  }

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, nb: Int): Fu[List[Tournament]] = coll.find(
    finishedSelect ++ BSONDocument(
      "schedule.freq" -> freq.name,
      "schedule.speed" -> BSONDocument("$in" -> Schedule.Speed.mostPopular.map(_.name))
    )
  ).sort(BSONDocument("startsAt" -> -1)).toList[Tournament](nb.some)

  def update(tour: Tournament) = coll.update(BSONDocument("_id" -> tour.id), tour)

  def insert(tour: Tournament) = coll.insert(tour)

  def remove(tour: Tournament) = coll.remove(BSONDocument("_id" -> tour.id))

  def exists(id: String) = coll.db command Count(coll.name, BSONDocument("_id" -> id).some) map (0 !=)

  def isFinished(id: String): Fu[Boolean] =
    coll.db command Count(coll.name, BSONDocument("_id" -> id, "status" -> Status.Finished.id).some) map (0 !=)
}
