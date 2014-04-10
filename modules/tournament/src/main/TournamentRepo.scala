package lila.tournament

import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import reactivemongo.bson.BSONDocument

import lila.db.api._
import lila.db.Implicits._
import tube.tournamentTube

object TournamentRepo {

  private def asCreated(tour: Tournament): Option[Created] = tour.some collect {
    case t: Created => t
  }
  private def asStarted(tour: Tournament): Option[Started] = tour.some collect {
    case t: Started => t
  }
  private def asFinished(tour: Tournament): Option[Finished] = tour.some collect {
    case t: Finished => t
  }

  def byId(id: String): Fu[Option[Tournament]] = $find byId id

  def nameById(id: String): Fu[Option[String]] =
    $primitive.one($select(id), "name")(_.asOpt[String])

  def createdById(id: String): Fu[Option[Created]] = byIdAs(id, asCreated)

  def startedById(id: String): Fu[Option[Started]] = byIdAs(id, asStarted)

  def createdByIdAndCreator(id: String, userId: String): Fu[Option[Created]] =
    createdById(id) map (_ filter (_ isCreator userId))

  private def byIdAs[A <: Tournament](id: String, as: Tournament => Option[A]): Fu[Option[A]] =
    $find byId id map (_ flatMap as)

  def created: Fu[List[Created]] = $find(
    $query(Json.obj(
      "status" -> Status.Created.id,
      "schedule" -> $exists(false)
    )) sort $sort.createdDesc
  ) map { _.map(asCreated).flatten }

  def started: Fu[List[Started]] = $find(
    $query(Json.obj("status" -> Status.Started.id)) sort $sort.createdDesc
  ) map { _.map(asStarted).flatten }

  def finished(limit: Int): Fu[List[Finished]] = $find(
    $query(Json.obj("status" -> Status.Finished.id)) sort $sort.createdDesc,
    limit
  ) map { _.map(asFinished).flatten }

  def enterable: Fu[List[Created]] = $find(
    $query(Json.obj(
      "status" -> Status.Created.id,
      "password" -> $exists(false)
    ) ++ $or(Seq(
        Json.obj("schedule" -> $exists(false)),
        Json.obj("schedule.at" -> $lt($date(DateTime.now plusMinutes 30)))
      ))) sort BSONDocument(
      "schedule.at" -> 1,
      "createdAt" -> 1
    )
  ) map { _.map(asCreated).flatten }

  def scheduled: Fu[List[Created]] = $find(
    $query(Json.obj(
      "status" -> Status.Created.id,
      "schedule" -> $exists(true)
    ))
  ) map { _.map(asCreated).flatten }

  def withdraw(userId: String): Fu[List[String]] = for {
    createds ← created
    createdIds ← (createds map (_ withdraw userId) collect {
      case scalaz.Success(tour) => $update(tour: Tournament) inject tour.id
    }).sequenceFu
    starteds ← started
    startedIds ← (starteds map (_ withdraw userId) collect {
      case scalaz.Success(tour) => $update(tour: Tournament) inject tour.id
    }).sequenceFu
  } yield createdIds ::: startedIds
}
