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

  def countDeclinedByTeam(teamId: ID): Fu[Int] =
    coll.countSel(teamDeclinedQuery(teamId))

  def findActiveByTeam(teamId: ID): Fu[List[Request]] =
    coll.list[Request](teamActiveQuery(teamId))

  def findActiveByTeams(teamIds: List[ID]): Fu[List[Request]] =
    teamIds.nonEmpty ?? coll.list[Request](teamsActiveQuery(teamIds))

  def selectId(teamId: ID, userId: ID)    = $id(Request.makeId(teamId, userId))
  def teamQuery(teamId: ID)               = $doc("team" -> teamId)
  def teamsQuery(teamIds: List[ID])       = $doc("team" $in teamIds)
  def teamDeclinedQuery(teamId: ID)       = $and(teamQuery(teamId), $doc("declined" -> true))
  def teamActiveQuery(teamId: ID)         = $and(teamQuery(teamId), $doc("declined" -> $ne(true)))
  def teamsActiveQuery(teamIds: List[ID]) = $and(teamsQuery(teamIds), $doc("declined" -> $ne(true)))

  def getByUserId(userId: User.ID) =
    coll.list[Request]($doc("user" -> userId))

  def remove(id: ID) = coll.delete.one($id(id))

  def cancel(teamId: ID, user: User): Fu[Boolean] =
    coll.delete.one(selectId(teamId, user.id)).map(_.n == 1)

  def removeByTeam(teamId: ID) = coll.delete.one(teamQuery(teamId))

  def removeByUser(userId: User.ID) = coll.delete.one($doc("user" -> userId))
}
