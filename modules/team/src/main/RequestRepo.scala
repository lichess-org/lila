package lila.team

import play.api.libs.json.Json
import reactivemongo.api._

import lila.db.JsTubeInColl
import lila.db.api._

trait RequestRepo {
  type ID = String
  implicit def inColl: JsTubeInColl[Request]

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
  def teamsQuery(teamIds: List[ID]) = Json.obj("team" -> $in(teamIds))
}

object RequestRepo extends RequestRepo {
  lazy val inColl = tube.requestTube
}
