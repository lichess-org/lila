package lila
package team

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports._
import scalaz.effects._
import org.joda.time.{ DateTime, Period }
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

  def exists(id: String): IO[Boolean] = byId(id) map (_.nonEmpty)

  def userHasCreatedSince(userId: String, duration: Period): IO[Boolean] = io {
    collection.find(
      ("createdAt" $gt (DateTime.now - duration)) +
        ("createdBy" -> userId)
    ).limit(1).size > 0
  }

  def incMembers(teamId: String, by: Int): IO[Unit] = io {
    update(selectId(teamId), $inc("nbMembers" -> by))
  }

  def userQuery(user: User) = DBObject("members.id" -> user.id)

  def selectId(id: String) = DBObject("_id" -> id)

  val enabledQuery = DBObject("enabled" -> true)

  val sortPopular = DBObject("nbMembers" -> -1)
}
