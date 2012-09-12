package lila
package tournament

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

class TournamentRepo(collection: MongoCollection)
    extends SalatDAO[RawTournament, String](collection) {

  def byId(id: String): IO[Option[Tournament]] = byIdAs(id, _.any)

  def createdById(id: String): IO[Option[Created]] = byIdAs(id, _.created)

  def startedById(id: String): IO[Option[Started]] = byIdAs(id, _.started)

  private def byIdAs[A](id: String, as: RawTournament => Option[A]): IO[Option[A]] = io {
    findOneById(id) flatMap as
  }

  def created: IO[List[Created]] = io {
    find(DBObject("status" -> Status.Created.id))
      .sort(DBObject("createdAt" -> -1))
      .toList.map(_.created).flatten
  }

  def started: IO[List[Started]] = io {
    find(DBObject("status" -> Status.Started.id))
      .sort(DBObject("createdAt" -> -1))
      .toList.map(_.started).flatten
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

  private def idSelector(id: String): DBObject = DBObject("_id" -> id)
  private def idSelector(tournament: Tournament): DBObject = idSelector(tournament.id)
}
