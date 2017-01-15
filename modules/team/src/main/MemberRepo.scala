package lila.team

import reactivemongo.bson._

import lila.db.dsl._

object MemberRepo {

  // dirty
  private val coll = Env.current.colls.member

  import BSONHandlers._

  type ID = String

  def userIdsByTeam(teamId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("user", $doc("team" -> teamId).some)

  def teamIdsByUser(userId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("team", $doc("user" -> userId).some)

  def removeByteam(teamId: ID): Funit =
    coll.remove(teamQuery(teamId)).void

  def removeByUser(userId: ID): Funit =
    coll.remove(userQuery(userId)).void

  def exists(teamId: ID, userId: ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def add(teamId: String, userId: String): Funit =
    coll.insert(Member.make(team = teamId, user = userId)).void

  def remove(teamId: String, userId: String): Funit =
    coll.remove(selectId(teamId, userId)).void

  def countByTeam(teamId: String): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def selectId(teamId: ID, userId: ID) = $id(Member.makeId(teamId, userId))
  def teamQuery(teamId: ID) = $doc("team" -> teamId)
  def userQuery(userId: ID) = $doc("user" -> userId)
}
