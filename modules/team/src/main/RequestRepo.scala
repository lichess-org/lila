package lila.team

import lila.db.dsl._

object RequestRepo {

  // dirty
  private val coll = Env.current.colls.request

  import BSONHandlers._

  type ID = String

  def exists(teamId: ID, userId: ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def find(teamId: ID, userId: ID): Fu[Option[Request]] =
    coll.uno[Request](selectId(teamId, userId))

  def countByTeam(teamId: ID): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def countByTeams(teamIds: List[ID]): Fu[Int] =
    coll.countSel(teamsQuery(teamIds))

  def findByTeam(teamId: ID): Fu[List[Request]] =
    coll.list[Request](teamQuery(teamId))

  def findByTeams(teamIds: List[ID]): Fu[List[Request]] =
    coll.list[Request](teamsQuery(teamIds))

  def selectId(teamId: ID, userId: ID) = $id(Request.makeId(teamId, userId))
  def teamQuery(teamId: ID) = $doc("team" -> teamId)
  def teamsQuery(teamIds: List[ID]) = $doc("team" $in teamIds)

  def getByUserId(userId: lila.user.User.ID) =
    coll.find($doc("user" -> userId)).list[Request]()

  def remove(id: ID) = coll.remove($id(id))
}
