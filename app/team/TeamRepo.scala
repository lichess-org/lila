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

  def byId(id: String): IO[Option[Team]] = io { findOneById(id) }

  def owned(id: String, createdBy: String): IO[Option[Team]] = io {
    findOne(selectId(id) ++ ("createdBy" -> createdBy))
  }

  def byOrderedIds(ids: Iterable[String]): IO[List[Team]] = io {
    find("_id" $in ids).toList
  } map { ts ⇒
    val tsMap = ts.map(u ⇒ u.id -> u).toMap
    ids.map(tsMap.get).flatten.toList
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

  def name(id: String): IO[Option[String]] = io {
    primitiveProjection[String](selectId(id), "name")
  }

  def userHasCreatedSince(userId: String, duration: Period): IO[Boolean] = io {
    collection.find(
      ("createdAt" $gt (DateTime.now - duration)) +
        ("createdBy" -> userId)
    ).limit(1).size > 0
  }

  def incMembers(teamId: String, by: Int): IO[Unit] = io {
    update(selectId(teamId), $inc("nbMembers" -> by))
  }

  def enable(team: Team) = updateIO(team)($set("enabled" -> true))

  def disable(team: Team) = updateIO(team)($set("enabled" -> false))

  def updateIO(teamId: String)(op: Team ⇒ DBObject): IO[Unit] = for {
    teamOption ← byId(teamId)
    _ ← ~teamOption.map(team ⇒ updateIO(team)(op(team)))
  } yield ()

  def updateIO(team: Team)(obj: DBObject): IO[Unit] = io {
    update(selectId(team), obj)
  }

  def addRequest(teamId: String, request: Request): IO[Unit] = io {
    update(
      selectId(teamId) ++ ("requests.user" $ne request.user), 
      $push("requests" -> request.user))
  }

  def selectId(id: String): DBObject = DBObject("_id" -> id)
  def selectId(team: Team): DBObject = selectId(team.id)

  val enabledQuery = DBObject("enabled" -> true)

  val sortPopular = DBObject("nbMembers" -> -1)
}
