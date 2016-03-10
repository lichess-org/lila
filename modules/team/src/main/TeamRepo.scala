package lila.team

import org.joda.time.{ DateTime, Period }
import play.api.libs.json.Json
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import reactivemongo.api._
import reactivemongo.bson._

import lila.db.api._
import lila.user.User
import tube.teamTube

object TeamRepo {

  type ID = String

  def owned(id: String, createdBy: String): Fu[Option[Team]] =
    $find.one($select(id) ++ Json.obj("createdBy" -> createdBy))

  def teamIdsByCreator(userId: String): Fu[List[String]] =
    teamTube.coll.distinct("_id", BSONDocument("createdBy" -> userId).some) map lila.db.BSON.asStrings

  def name(id: String): Fu[Option[String]] =
    $primitive.one($select(id), "name")(_.asOpt[String])

  def userHasCreatedSince(userId: String, duration: Period): Fu[Boolean] =
    $count.exists(Json.obj(
      "createdAt" -> $gt($date(DateTime.now minus duration)),
      "createdBy" -> userId
    ))

  def ownerOf(teamId: String): Fu[Option[String]] =
    $primitive.one($select(teamId), "createdBy")(_.asOpt[String])

  def incMembers(teamId: String, by: Int): Funit =
    $update($select(teamId), $inc("nbMembers" -> by))

  def enable(team: Team) = $update.field(team.id, "enabled", true)

  def disable(team: Team) = $update.field(team.id, "enabled", false)

  def addRequest(teamId: String, request: Request): Funit =
    $update(
      $select(teamId) ++ Json.obj("requests.user" -> $ne(request.user)),
      $push("requests", request.user))

  val enabledQuery = Json.obj("enabled" -> true)

  val sortPopular = $sort desc "nbMembers"
}
