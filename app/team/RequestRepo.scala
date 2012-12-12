package lila
package team

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

import scalaz.effects._
import org.joda.time.DateTime

// db.team_request.ensureIndex({team:1})
// db.team_request.ensureIndex({date: -1})
final class RequestRepo(collection: MongoCollection)
    extends SalatDAO[Request, String](collection) {

  def byId(id: String): IO[Option[Request]] = io { findOneById(id) }

  def exists(teamId: String, userId: String): IO[Boolean] = io {
    collection.find(idQuery(teamId, userId)).limit(1).size != 0
  }

  def find(teamId: String, userId: String): IO[Option[Request]] = io {
    findOneById(id(teamId, userId))
  }

  def countByTeamId(teamId: String): IO[Int] = io {
    count(teamIdQuery(teamId)).toInt
  }

  def findByTeamId(teamId: String): IO[List[Request]] = io {
    find(teamIdQuery(teamId)).toList
  }

  def idQuery(teamId: String, userId: String) = DBObject("_id" -> id(teamId, userId))
  def idQuery(id: String) = DBObject("_id" -> id)
  def id(teamId: String, userId: String) = Request.makeId(teamId, userId)
  def teamIdQuery(teamId: String) = DBObject("team" -> teamId)
  def sortQuery(order: Int = -1) = DBObject("date" -> order)

  def add(request: Request): IO[Unit] = io { insert(request) }

  def remove(id: String): IO[Unit] = io { remove(idQuery(id)) }
}
