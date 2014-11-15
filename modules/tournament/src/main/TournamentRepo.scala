package lila.tournament

import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDocument, BSONArray }
import reactivemongo.core.commands.Count

import BSONHandlers._
import lila.db.BSON.BSONJodaDateTimeHandler
import lila.db.Implicits._

object TournamentRepo {

  private lazy val coll = Env.current.tournamentColl

  private def byIdQuery(id: String) = coll.find(BSONDocument("_id" -> id))

  def byId(id: String): Fu[Option[Tournament]] = byIdQuery(id).one[Tournament]

  def createdById(id: String): Fu[Option[Created]] = byIdQuery(id).one[Created]

  def enterableById(id: String): Fu[Option[Enterable]] = byIdQuery(id).one[Enterable]

  def startedById(id: String): Fu[Option[Started]] = byIdQuery(id).one[Started]

  def createdByIdAndCreator(id: String, userId: String): Fu[Option[Created]] =
    createdById(id) map (_ filter (_ isCreator userId))

  def allEnterable: Fu[List[Enterable]] = coll.find(BSONDocument(
    "status" -> BSONDocument("$in" -> List(Status.Created.id, Status.Started.id))
  )).toList[Enterable](None)

  def created: Fu[List[Created]] = coll.find(BSONDocument(
    "status" -> Status.Created.id,
    "schedule" -> BSONDocument("$exists" -> false)
  )).toList[Created](None)

  def started: Fu[List[Started]] = coll.find(BSONDocument(
    "status" -> Status.Started.id
  )).sort(BSONDocument("createdAt" -> -1)).toList[Started](None)

  def finished(limit: Int): Fu[List[Finished]] = coll.find(BSONDocument(
    "status" -> Status.Finished.id
  )).sort(BSONDocument("startedAt" -> -1)).toList[Finished](limit.some)

  private def allCreatedSelect = BSONDocument(
    "status" -> Status.Created.id,
    "$or" -> BSONArray(
      BSONDocument("schedule" -> BSONDocument("$exists" -> false)),
      BSONDocument("schedule.at" -> BSONDocument("$lt" -> (DateTime.now plusMinutes 30)))
    )
  )

  def noPasswordCreatedSorted: Fu[List[Created]] = coll.find(
    allCreatedSelect ++ BSONDocument("password" -> BSONDocument("$exists" -> false))
  ).sort(BSONDocument("schedule.at" -> 1, "createdAt" -> 1)).toList[Created](None)

  def allCreated: Fu[List[Created]] = coll.find(allCreatedSelect).toList[Created](None)

  def recentlyStartedSorted: Fu[List[Started]] = coll.find(BSONDocument(
    "status" -> Status.Started.id,
    "password" -> BSONDocument("$exists" -> false),
    "startedAt" -> BSONDocument("$gt" -> (DateTime.now minusMinutes 20))
  )).sort(BSONDocument("schedule.at" -> 1, "createdAt" -> 1)).toList[Started](none)

  def promotable: Fu[List[Enterable]] = noPasswordCreatedSorted zip recentlyStartedSorted map {
    case (created, started) => created ::: started
  }

  def scheduled: Fu[List[Created]] = coll.find(BSONDocument(
    "status" -> Status.Created.id,
    "schedule" -> BSONDocument("$exists" -> true)
  )).sort(BSONDocument("schedule.at" -> 1)).toList[Created](none)

  def lastFinishedScheduledByFreq(freq: Schedule.Freq, nb: Int): Fu[List[Finished]] = coll.find(
    BSONDocument(
      "status" -> Status.Finished.id,
      "schedule.freq" -> freq.name
    )
  ).sort(BSONDocument("schedule.at" -> -1)).toList[Finished](nb.some)

  def update(tour: Tournament) = coll.update(BSONDocument("_id" -> tour.id), tour)

  def insert(tour: Tournament) = coll.insert(tour)

  def remove(tour: Tournament) = coll.remove(BSONDocument("_id" -> tour.id))

  def exists(id: String) = coll.db command Count(coll.name, BSONDocument("_id" -> id).some) map (0 !=)

  def withdraw(userId: String): Fu[List[String]] = for {
    createds ← created
    createdIds ← (createds map (_ withdraw userId) collect {
      case scalaz.Success(tour) => update(tour) inject tour.id
    }).sequenceFu
    starteds ← started
    startedIds ← (starteds map (_ withdraw userId) collect {
      case scalaz.Success(tour) => update(tour) inject tour.id
    }).sequenceFu
  } yield createdIds ::: startedIds
}
