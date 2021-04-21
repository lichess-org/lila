package lila.team

import lila.db.dsl._
import lila.user.User

final class RequestRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  type ID = String

  def exists(teamId: ID, userId: ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def find(teamId: ID, userId: ID): Fu[Option[Request]] =
    coll.one[Request](selectId(teamId, userId))

  def countByTeam(teamId: ID): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def findByTeam(teamId: ID): Fu[List[Request]] =
    coll.list[Request](teamQuery(teamId))

  def findByTeams(teamIds: List[ID]): Fu[List[Request]] =
    teamIds.nonEmpty ?? coll.list[Request](teamsQuery(teamIds))

  def selectId(teamId: ID, userId: ID) = $id(Request.makeId(teamId, userId))
  def teamQuery(teamId: ID)            = $doc("team" -> teamId)
  def teamsQuery(teamIds: List[ID])    = $doc("team" $in teamIds)

  def getByUserId(userId: User.ID) =
    coll.list[Request]($doc("user" -> userId))

  def remove(id: ID) = coll.delete.one($id(id))

  def cancel(teamId: ID, user: User): Fu[Boolean] =
    coll.delete.one(selectId(teamId, user.id)).map(_.n == 1)

  def removeByTeam(teamId: ID) = coll.delete.one(teamQuery(teamId))

  def removeByUser(userId: User.ID) = coll.delete.one($doc("user" -> userId))
}
