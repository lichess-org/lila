package lila.team

import lila.db.dsl._
import reactivemongo.api.Cursor

object RequestRepo {

  // dirty
  private val coll = Env.current.colls.request

  import BSONHandlers._

  type ID = String

  def exists(teamId: ID, userId: ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def find(teamId: ID, userId: ID): Fu[Option[Request]] =
    coll.find(selectId(teamId, userId)).one[Request]

  def countByTeam(teamId: ID): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def countByTeams(teamIds: List[ID]): Fu[Int] =
    coll.countSel(teamsQuery(teamIds))

  def findByTeam(teamId: ID): Fu[List[Request]] =
    coll.find(teamQuery(teamId)).cursor[Request]().list

  def findByTeams(teamIds: List[ID]): Fu[List[Request]] =
    coll.find(teamsQuery(teamIds)).cursor[Request]().list

  def selectId(teamId: ID, userId: ID) = $id(Request.makeId(teamId, userId))
  def teamQuery(teamId: ID) = $doc("team" -> teamId)
  def teamsQuery(teamIds: List[ID]) = $doc("team" $in teamIds)

  def getByUserId(userId: lila.user.User.ID): Fu[List[Request]] =
    coll.find($doc("user" -> userId)).cursor[Request]().list

  def remove(id: ID) = coll.delete.one($id(id))
}
