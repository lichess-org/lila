package lila.team

import lila.db.api._

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._

// db.team_member.ensureIndex({team:1})
// db.team_member.ensureIndex({user:1})
// db.team_member.ensureIndex({date: -1})
private[team] object MemberRepo {

  type ID = String

  private implicit def tube = memberTube

  def userIdsByTeam(teamId: String): Fu[List[String]] = 
    $primitive(teamQuery(teamId), "user")(_.asOpt[String])

  def teamIdsByUser(userId: String): Fu[List[String]] = 
    $primitive(userQuery(userId), "team")(_.asOpt[String])

  def removeByteam(teamId: String): Fu[Unit] = 
    $remove(teamQuery(teamId))

  def removeByUser(userId: String): Fu[Unit] = 
    $remove(userQuery(userId))

  def exists(teamId: String, userId: String): Fu[Boolean] = 
    $count.exists(selectId(teamId, userId))

  def selectId(teamId: String, userId: String) = $select(Members.makeId(teamId, userId))
  def teamQuery(teamId: String) = Json.obj("team" -> teamId)
  def userQuery(userId: String) = Json.obj("user" -> userId)
}
