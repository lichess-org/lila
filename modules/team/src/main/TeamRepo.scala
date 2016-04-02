package lila.team

import org.joda.time.{ DateTime, Period }
import reactivemongo.api._
import reactivemongo.bson._

import lila.db.dsl._
import lila.user.{ User, UserRepo }

object TeamRepo {

  // dirty
  private val coll = Env.current.colls.team

  import BSONHandlers._

  type ID = String

  def byOrderedIds(ids: Seq[String]) = coll.byOrderedIds[Team](ids)(_.id)

  def cursor(selector: Bdoc) = coll.find(selector).cursor[Team]()

  def owned(id: String, createdBy: String): Fu[Option[Team]] =
    coll.one[Team]($id(id) ++ $doc("createdBy" -> createdBy))

  def teamIdsByCreator(userId: String): Fu[List[String]] =
    coll.distinct("_id", BSONDocument("createdBy" -> userId).some) map lila.db.BSON.asStrings

  def name(id: String): Fu[Option[String]] =
    coll.primitiveOne[String]($id(id), "name")

  def userHasCreatedSince(userId: String, duration: Period): Fu[Boolean] =
    coll.exists($doc(
      "createdAt" $gt DateTime.now.minus(duration),
      "createdBy" -> userId
    ))

  def ownerOf(teamId: String): Fu[Option[String]] =
    coll.primitiveOne[String]($id(teamId), "createdBy")

  def incMembers(teamId: String, by: Int): Funit =
    coll.update($id(teamId), $inc("nbMembers" -> by)).void

  def enable(team: Team) = coll.updateField($id(team.id), "enabled", true)

  def disable(team: Team) = coll.updateField($id(team.id), "enabled", false)

  def addRequest(teamId: String, request: Request): Funit =
    coll.update(
      $id(teamId) ++ $doc("requests.user" $ne request.user),
      $push("requests", request.user)).void

  val enabledQuery = $doc("enabled" -> true)

  val sortPopular = $sort desc "nbMembers"
}
