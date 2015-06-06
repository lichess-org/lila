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

  def createdById(id: String): Fu[Option[Created]] =
    coll.find(selectId(id) ++ createdSelect).one[Created]

  def enterableById(id: String): Fu[Option[Enterable]] =
    coll.find(selectId(id) ++ enterableSelect).one[Enterable]

  def startedById(id: String): Fu[Option[Started]] =
    coll.find(selectId(id) ++ startedSelect).one[Started]

  def startedOrFinishedById(id: String): Fu[Option[StartedOrFinished]] =
    byId(id) map {
      case Some(t: StartedOrFinished) => t.some
      case _                          => none
    }

  def createdByIdAndCreator(id: String, userId: String): Fu[Option[Created]] =
    createdById(id) map (_ filter (_ isCreator userId))

  def allEnterable: Fu[List[Enterable]] = coll.find(enterableSelect).toList[Enterable](None)

  def created: Fu[List[Created]] = coll.find(createdSelect ++ BSONDocument(
    "schedule" -> BSONDocument("$exists" -> false)
  )).toList[Created](None)

  def createdIncludingScheduled: Fu[List[Created]] = coll.find(createdSelect).toList[Created](None)

  def started: Fu[List[Started]] =
    coll.find(startedSelect).sort(BSONDocument("createdAt" -> -1)).toList[Started](None)

  def publicStarted: Fu[List[Started]] =
    coll.find(startedSelect ++ BSONDocument("private" -> BSONDocument("$exists" -> false))).sort(BSONDocument("createdAt" -> -1)).toList[Started](None)

  def finished(limit: Int): Fu[List[Finished]] =
    coll.find(finishedSelect).sort(BSONDocument("startedAt" -> -1)).toList[Finished](limit.some)

  private def allCreatedSelect = createdSelect ++ BSONDocument(
    "$or" -> BSONArray(
      BSONDocument("schedule" -> BSONDocument("$exists" -> false)),
      BSONDocument("schedule.at" -> BSONDocument("$lt" -> (DateTime.now plusMinutes 30)))
    )
  )

  def publicCreatedSorted: Fu[List[Created]] = coll.find(
    allCreatedSelect ++ BSONDocument("private" -> BSONDocument("$exists" -> false))
  ).sort(BSONDocument("schedule.at" -> 1, "createdAt" -> 1)).toList[Created](None)

  def allCreated: Fu[List[Created]] = coll.find(allCreatedSelect).toList[Created](None)

  private def notCloseToFinishSorted: Fu[List[Started]] = {
    val finishAfter = DateTime.now plusMinutes 20
    coll.find(startedSelect ++ BSONDocument(
      "private" -> BSONDocument("$exists" -> false)
    )).sort(BSONDocument("schedule.at" -> 1, "createdAt" -> 1)).toList[Started](none) map {
      _.filter(_.finishedAt isAfter finishAfter)
    }
  }

  def promotable: Fu[List[Enterable]] = publicCreatedSorted zip notCloseToFinishSorted map {
    case (created, started) => created ::: started
  }

  def scheduled: Fu[List[Created]] = coll.find(createdSelect ++ BSONDocument(
    "schedule" -> BSONDocument("$exists" -> true)
  )).sort(BSONDocument("schedule.at" -> 1)).toList[Created](none)

  def scheduledDedup: Fu[List[Created]] = scheduled map {
    import Schedule.Freq
    _.flatMap { tour =>
      tour.schedule map (tour -> _)
    }.foldLeft(List[Created]() -> none[Freq]) {
      case ((tours, skip), (_, sched)) if skip.contains(sched.freq) => (tours, skip)
      case ((tours, skip), (tour, sched)) => (tour :: tours, sched.freq match {
        case Freq.Daily   => Freq.Nightly.some
        case Freq.Nightly => Freq.Daily.some
        case _            => skip
      })
    }._1.reverse
  }

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, nb: Int): Fu[List[Finished]] = coll.find(
    finishedSelect ++ BSONDocument(
      "schedule.freq" -> freq.name,
      "schedule.speed" -> BSONDocument("$in" -> Schedule.Speed.noSuperBlitz.map(_.name))
    )
  ).sort(BSONDocument("schedule.at" -> -1)).toList[Finished](nb.some)

  def update(tour: Tournament) = coll.update(BSONDocument("_id" -> tour.id), tour)

  def insert(tour: Tournament) = coll.insert(tour)

  def remove(tour: Tournament) = coll.remove(BSONDocument("_id" -> tour.id))

  def exists(id: String) = coll.db command Count(coll.name, BSONDocument("_id" -> id).some) map (0 !=)

  def withdraw(userId: String): Fu[List[String]] = for {
    createds ← createdIncludingScheduled
    createdIds ← (createds map (_ withdraw userId) collect {
      case scalaz.Success(tour) => update(tour) inject tour.id
    }).sequenceFu
    starteds ← started
    startedIds ← (starteds map (_ withdraw userId) collect {
      case scalaz.Success(tour) => update(tour) inject tour.id
    }).sequenceFu
  } yield createdIds ::: startedIds
}
