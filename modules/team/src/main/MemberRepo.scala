package lila.team

import reactivemongo.api.bson.*
import reactivemongo.api.commands.WriteResult

import lila.db.dsl.{ *, given }
import lila.user.User

final class MemberRepo(val coll: Coll)(using ec: scala.concurrent.ExecutionContext):

  import BSONHandlers.given

  // expensive with thousands of members!
  def userIdsByTeam(teamId: TeamId): Fu[List[User.ID]] =
    coll.secondaryPreferred.distinctEasy[User.ID, List]("user", $doc("team" -> teamId))

  def removeByTeam(teamId: TeamId): Funit =
    coll.delete.one(teamQuery(teamId)).void

  def removeByUser(userId: User.ID): Funit =
    coll.delete.one(userQuery(userId)).void

  def exists(teamId: TeamId, userId: User.ID): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def add(teamId: TeamId, userId: User.ID): Funit =
    coll.insert.one(TeamMember.make(team = teamId, user = userId)).void

  def remove(teamId: TeamId, userId: User.ID): Fu[WriteResult] =
    coll.delete.one(selectId(teamId, userId))

  def countByTeam(teamId: TeamId): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def filterUserIdsInTeam(teamId: TeamId, userIds: Iterable[User.ID]): Fu[Set[User.ID]] =
    userIds.nonEmpty ??
      coll.distinctEasy[User.ID, Set]("user", $inIds(userIds.map { TeamMember.makeId(teamId, _) }))

  def isSubscribed(team: Team, user: User): Fu[Boolean] =
    !coll.exists(selectId(team.id, user.id) ++ $doc("unsub" -> true))

  def subscribe(teamId: TeamId, userId: User.ID, v: Boolean): Funit =
    coll.update
      .one(
        selectId(teamId, userId),
        if (v) $unset("unsub")
        else $set("unsub" -> true)
      )
      .void

  private[team] def countUnsub(teamId: TeamId): Fu[Int] =
    coll.countSel(teamQuery(teamId) ++ $doc("unsub" -> true))

  def teamQuery(teamId: TeamId)                         = $doc("team" -> teamId)
  private def selectId(teamId: TeamId, userId: User.ID) = $id(TeamMember.makeId(teamId, userId))
  private def userQuery(userId: User.ID)                 = $doc("user" -> userId)
