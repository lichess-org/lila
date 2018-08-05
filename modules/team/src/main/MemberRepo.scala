package lila.team

import reactivemongo.api.ReadConcern
import reactivemongo.bson._

import lila.db.dsl._

object MemberRepo {

  // dirty
  private val coll = Env.current.colls.member

  import BSONHandlers._

  type ID = String

  def userIdsByTeam(teamId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("user", $doc("team" -> teamId))

  def teamIdsByUser(userId: ID): Fu[Set[ID]] =
    coll.distinct[String, Set]("team", $doc("user" -> userId))

  def removeByteam(teamId: ID): Funit =
    coll.delete.one(teamQuery(teamId)).void

  def removeByUser(userId: ID): Funit =
    coll.delete.one(userQuery(userId)).void

  def exists(teamId: ID, userId: ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def add(teamId: String, userId: String): Funit =
    coll.insert.one(Member.make(team = teamId, user = userId)).void

  def remove(teamId: String, userId: String): Funit =
    coll.delete.one(selectId(teamId, userId)).void

  def countByTeam(teamId: String): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  private def selectId(teamId: ID, userId: ID) =
    $id(Member.makeId(teamId, userId))

  private[team] def teamQuery(teamId: ID) = $doc("team" -> teamId)

  private def userQuery(userId: ID) = $doc("user" -> userId)
}
