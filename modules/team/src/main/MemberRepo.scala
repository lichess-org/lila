package lila.team

import reactivemongo.api.bson._
import reactivemongo.api.commands.WriteResult

import lila.db.dsl._
import lila.user.User

final class MemberRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  type ID = String

  // expensive with thousands of members!
  def userIdsByTeam(teamId: ID): Fu[Set[ID]] =
    coll.secondaryPreferred.distinctEasy[String, Set]("user", $doc("team" -> teamId))

  def teamIdsByUser(userId: User.ID): Fu[Set[ID]] =
    coll.distinctEasy[ID, Set]("team", $doc("user" -> userId))

  def removeByteam(teamId: ID): Funit =
    coll.delete.one(teamQuery(teamId)).void

  def removeByUser(userId: User.ID): Funit =
    coll.delete.one(userQuery(userId)).void

  def exists(teamId: ID, userId: User.ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def add(teamId: ID, userId: User.ID): Funit =
    coll.insert.one(Member.make(team = teamId, user = userId)).void

  def remove(teamId: ID, userId: User.ID): Fu[WriteResult] =
    coll.delete.one(selectId(teamId, userId))

  def countByTeam(teamId: ID): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def filterUserIdsInTeam(teamId: ID, userIds: Set[User.ID]): Fu[Set[User.ID]] =
    userIds.nonEmpty ??
      coll.distinctEasy[User.ID, Set]("user", $inIds(userIds.map { Member.makeId(teamId, _) }))

  def teamQuery(teamId: ID)                    = $doc("team" -> teamId)
  private def selectId(teamId: ID, userId: ID) = $id(Member.makeId(teamId, userId))
  private def userQuery(userId: ID)            = $doc("user" -> userId)
}
