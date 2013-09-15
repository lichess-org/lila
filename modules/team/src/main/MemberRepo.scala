package lila.team

import play.api.libs.json.Json
import reactivemongo.api._

import lila.db.api._
import tube.memberTube

object MemberRepo {

  type ID = String

  def userIdsByTeam(teamId: ID): Fu[List[ID]] = 
    $primitive(teamQuery(teamId), "user")(_.asOpt[ID])

  def teamIdsByUser(userId: ID): Fu[List[ID]] = 
    $primitive(userQuery(userId), "team")(_.asOpt[ID])

  def removeByteam(teamId: ID): Funit = 
    $remove(teamQuery(teamId))

  def removeByUser(userId: ID): Funit = 
    $remove(userQuery(userId))

  def exists(teamId: ID, userId: ID): Fu[Boolean] = 
    $count.exists(selectId(teamId, userId))

  def add(teamId: String, userId: String): Funit = 
    $insert(Member.make(team = teamId, user = userId))

  def remove(teamId: String, userId: String): Funit = 
    $remove(selectId(teamId, userId))

  def selectId(teamId: ID, userId: ID) = $select(Member.makeId(teamId, userId))
  def teamQuery(teamId: ID) = Json.obj("team" -> teamId)
  def userQuery(userId: ID) = Json.obj("user" -> userId)
}
