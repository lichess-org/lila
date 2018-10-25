package lila.team

import org.joda.time.{ DateTime, Period }
import reactivemongo.api._
import reactivemongo.bson._

import lila.db.dsl._
import lila.user.User

object TeamRepo {

  // dirty
  private val coll = Env.current.colls.team

  import BSONHandlers._

  def byOrderedIds(ids: Seq[Team.ID]) = coll.byOrderedIds[Team, Team.ID](ids)(_.id)

  def cursor(
    selector: Bdoc,
    readPreference: ReadPreference = ReadPreference.secondaryPreferred
  )(
    implicit
    cp: CursorProducer[Team]
  ) =
    coll.find(selector).cursor[Team](readPreference)

  def owned(id: Team.ID, createdBy: User.ID): Fu[Option[Team]] =
    coll.uno[Team]($id(id) ++ $doc("createdBy" -> createdBy))

  def teamIdsByCreator(userId: User.ID): Fu[List[String]] =
    coll.distinct[String, List]("_id", $doc("createdBy" -> userId).some)

  def creatorOf(teamId: Team.ID): Fu[Option[User.ID]] =
    coll.primitiveOne[User.ID]($id(teamId), "_id")

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
      $push("requests", request.user)
    ).void

  def changeOwner(teamId: String, newOwner: User.ID) =
    coll.updateField($id(teamId), "createdBy", newOwner)

  val enabledQuery = $doc("enabled" -> true)

  val sortPopular = $sort desc "nbMembers"
}
