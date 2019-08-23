package lidraughts.team

import reactivemongo.bson._

import lidraughts.db.dsl._
import lidraughts.user.User

object MemberRepo {

  // dirty
  private val coll = Env.current.colls.member

  import BSONHandlers._

  type ID = String

  // expensive with thousands of members!
  def userIdsByTeam(teamId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("user", $doc("team" -> teamId).some)

  def teamIdsByUser(userId: User.ID): Fu[Set[ID]] =
    coll.distinct[ID, Set]("team", $doc("user" -> userId).some)

  def removeByteam(teamId: ID): Funit =
    coll.remove(teamQuery(teamId)).void

  def removeByUser(userId: User.ID): Funit =
    coll.remove(userQuery(userId)).void

  def exists(teamId: ID, userId: User.ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def add(teamId: ID, userId: User.ID): Funit =
    coll.insert(Member.make(team = teamId, user = userId)).void

  def remove(teamId: ID, userId: User.ID): Funit =
    coll.remove(selectId(teamId, userId)).void

  def countByTeam(teamId: ID): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def selectId(teamId: ID, userId: ID) = $id(Member.makeId(teamId, userId))
  def teamQuery(teamId: ID) = $doc("team" -> teamId)
  def userQuery(userId: ID) = $doc("user" -> userId)
}
