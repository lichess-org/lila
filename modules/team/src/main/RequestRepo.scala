package lila.team

import lila.db.api._
import tube.requestTube

import play.api.libs.json.Json

import reactivemongo.api._

// db.team_request.ensureIndex({team:1})
// db.team_request.ensureIndex({date: -1})
object RequestRepo {

  type ID = String

  def exists(teamId: ID, userId: ID): Fu[Boolean] = 
    $count.exists(selectId(teamId, userId))

  def find(teamId: ID, userId: ID): Fu[Option[Request]] = 
    $find.one(selectId(teamId, userId))

  def countByTeam(teamId: ID): Fu[Int] = 
    $count(teamQuery(teamId))

  def countByTeams(teamIds: List[ID]): Fu[Int] = 
    $count(teamsQuery(teamIds))

  def findByTeam(teamId: ID): Fu[List[Request]] = 
    $find(teamQuery(teamId))

  def findByTeams(teamIds: List[ID]): Fu[List[Request]] = 
    $find(teamsQuery(teamIds))

  def selectId(teamId: ID, userId: ID) = $select(Request.makeId(teamId, userId))
  def teamQuery(teamId: ID) = Json.obj("team" -> teamId)
  def teamsQuery(teamIds: List[ID]) = Json.obj("team" -> $in(teamIds: _*))
}
