package lila
package team

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

import user.User

final class TeamRepo(collection: MongoCollection)
    extends SalatDAO[Team, String](collection) {

  def byId(id: String): IO[Option[Team]] = io {
    findOneById(id)
  }

  def byUser(user: User): IO[List[Team]] = io {
    find(userQuery(user)).sort(sortPopular).toList
  }

  def saveIO(team: Team): IO[Unit] = io {
    update(
      selectId(team.id),
      _grater asDBObject team,
      upsert = true)
  }

  def removeIO(team: Team): IO[Unit] = io {
    remove(selectId(team.id))
  }

  def userQuery(user: User) = DBObject("members.id" -> user.id)

  def selectId(id: String) = DBObject("_id" -> id)

  val queryAll = DBObject()

  val sortPopular = DBObject("nbMembers" -> -1)
}
