package lila
package tournament

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._
import scalaz.Success
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

import user.User

class TournamentRepo(collection: MongoCollection)
    extends SalatDAO[RawTournament, String](collection) {

  def byId(id: String): IO[Option[Tournament]] = byIdAs(id, _.any)

  def createdById(id: String): IO[Option[Created]] = byIdAs(id, _.created)

  def startedById(id: String): IO[Option[Started]] = byIdAs(id, _.started)

  private def byIdAs[A](id: String, as: RawTournament ⇒ Option[A]): IO[Option[A]] = io {
    findOneById(id) flatMap as
  }

  val created: IO[List[Created]] = io {
    find(DBObject("status" -> Status.Created.id))
      .sort(DBObject("createdAt" -> -1))
      .toList.map(_.created).flatten
  }

  val started: IO[List[Started]] = io {
    find(DBObject("status" -> Status.Started.id))
      .sort(DBObject("createdAt" -> -1))
      .toList.map(_.started).flatten
  }

  def finished(limit: Int): IO[List[Finished]] = io {
    find(DBObject("status" -> Status.Finished.id))
      .sort(DBObject("createdAt" -> -1))
      .limit(limit)
      .toList.map(_.finished).flatten
  }

  def setUsers(tourId: String, userIds: List[String]): IO[Unit] = io {
    update(idSelector(tourId), $set("data.users" -> userIds.distinct))
  }

  def userHasRunningTournament(username: String): IO[Boolean] = io {
    collection.findOne(
      ("status" $ne Status.Finished.id) ++ ("data.users" -> username)
    ).isDefined
  }

  val inProgressIds: IO[List[String]] = io {
    primitiveProjections(DBObject("status" -> Status.Started.id), "_id")
  }

  def saveIO(tournament: Tournament): IO[Unit] = io {
    save(tournament.encode)
  }

  def removeIO(tournament: Tournament): IO[Unit] = io {
    remove(idSelector(tournament))
  }

  def withdraw(user: User): IO[List[String]] = for {
    createds ← created
    createdIds ← (createds map (_ withdraw user) collect {
      case Success(tour) ⇒ saveIO(tour) map (_ ⇒ tour.id)
    }).sequence
    starteds ← started
    startedIds ← (starteds map (_ withdraw user) collect {
      case Success(tour) ⇒ saveIO(tour) map (_ ⇒ tour.id)
    }).sequence
  } yield createdIds ::: startedIds

  private def idSelector(id: String): DBObject = DBObject("_id" -> id)
  private def idSelector(tournament: Tournament): DBObject = idSelector(tournament.id)
}
