package lila
package team

import com.novus.salat._
import com.novus.salat.dao._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._

import scalaz.effects._
import org.joda.time.DateTime

// db.member.ensureIndex({t:1})
// db.member.ensureIndex({u:1})
// db.member.ensureIndex({d: -1})
final class MemberRepo(collection: MongoCollection)
    extends SalatDAO[Member, String](collection) {

  // def byTeamId(teamId: String): IO[List[Member]] = io {
  //   find(teamIdQuery(teamId)).toList
  // }

  def userIdsByTeamId(teamId: String): IO[List[String]] = io {
    (collection find teamIdQuery(teamId) sort sortQuery(1) map { obj ⇒
      obj.getAs[String]("user")
    }).flatten.toList
  }

  def teamIdsByUserId(userId: String): IO[Set[String]] = io {
    (collection find userIdQuery(userId) map { obj ⇒
      obj.getAs[String]("team")
    }).flatten.toSet
  }

  def removeByteamId(teamId: String): IO[Unit] = io {
    remove(teamIdQuery(teamId))
  }

  def exists(teamId: String, userId: String): IO[Boolean] = io {
    collection.find(idQuery(teamId, userId)).limit(1).size != 0
  }

  def idQuery(teamId: String, userId: String) = DBObject("_id" -> id(teamId, userId))
  def id(teamId: String, userId: String) = Member.makeId(teamId, userId)
  def teamIdQuery(teamId: String) = DBObject("team" -> teamId)
  def userIdQuery(userId: String) = DBObject("user" -> userId)
  def sortQuery(order: Int = -1) = DBObject("date" -> order)

  def add(teamId: String, userId: String): IO[Unit] = io {
    insert(Member(team = teamId, user = userId))
  }

  def remove(teamId: String, userId: String): IO[Unit] = io {
    remove(idQuery(teamId, userId))
  }
}
