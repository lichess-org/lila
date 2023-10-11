package lila.team

import reactivemongo.api.bson.*
import reactivemongo.api.commands.WriteResult

import lila.db.dsl.{ *, given }
import lila.user.User

final class MemberRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  // expensive with thousands of members!
  def userIdsByTeam(teamId: TeamId): Fu[List[UserId]] =
    coll.secondaryPreferred.distinctEasy[UserId, List]("user", $doc("team" -> teamId))

  def removeByTeam(teamId: TeamId): Funit =
    coll.delete.one(teamQuery(teamId)).void

  def removeByUser(userId: UserId): Funit =
    coll.delete.one(userQuery(userId)).void

  def get[U: UserIdOf](teamId: TeamId, userId: U): Fu[Option[TeamMember]] =
    coll.byId[TeamMember](TeamMember.makeId(teamId, userId))

  def exists(teamId: TeamId, userId: UserId): Fu[Boolean] =
    coll.exists(selectId(teamId, userId))

  def add(teamId: TeamId, userId: UserId): Funit =
    coll.insert.one(TeamMember.make(team = teamId, user = userId)).void

  def remove(teamId: TeamId, userId: UserId): Fu[WriteResult] =
    coll.delete.one(selectId(teamId, userId))

  def countByTeam(teamId: TeamId): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  def filterUserIdsInTeam[U: UserIdOf](teamId: TeamId, users: Iterable[U]): Fu[Set[UserId]] =
    users.nonEmpty so
      coll.distinctEasy[UserId, Set]("user", $inIds(users.map { TeamMember.makeId(teamId, _) }))

  def isSubscribed[U: UserIdOf](team: Team, user: U): Fu[Boolean] =
    !coll.exists(selectId(team.id, user) ++ $doc("unsub" -> true))

  def subscribe(teamId: TeamId, userId: UserId, v: Boolean): Funit =
    coll.update
      .one(
        selectId(teamId, userId),
        if v then $unset("unsub")
        else $set("unsub" -> true)
      )
      .void

  def hasPerm[A: UserIdOf](teamId: TeamId, user: A, perm: TeamSecurity.Permission.Selector): Fu[Boolean] =
    coll.exists(selectId(teamId, user) ++ $doc("perms" -> perm(TeamSecurity.Permission)))

  def hasAnyPerm[A: UserIdOf](teamId: TeamId, user: A): Fu[Boolean] =
    coll.exists(selectId(teamId, user) ++ selectAnyPerm)

  def setPerms[A: UserIdOf](teamId: TeamId, user: A, perms: Set[TeamSecurity.Permission]): Funit =
    coll
      .updateOrUnsetField(
        $id(TeamMember.makeId(teamId, user.id)),
        "perms",
        perms.nonEmpty option perms
      )
      .void

  def leaders(teamId: TeamId): Fu[List[TeamMember]] =
    coll
      .find(teamQuery(teamId) ++ selectAnyPerm)
      .sort($doc("_id" -> 1))
      .cursor[TeamMember]()
      .listAll()

  def leaderIds(teamId: TeamId): Fu[Set[UserId]] =
    coll.primitive[UserId](teamQuery(teamId) ++ selectAnyPerm, "user").dmap(_.toSet)

  def publicLeaderIds(teamId: TeamId): Fu[List[UserId]] =
    coll.primitive[UserId](
      teamQuery(teamId) ++ $doc("perms" -> TeamSecurity.Permission.Public),
      "user"
    )

  def leadsOneOf(userId: UserId, teamIds: Iterable[TeamId]): Fu[Boolean] = teamIds.nonEmpty so
    coll.secondaryPreferred.exists($inIds(teamIds.map(TeamMember.makeId(_, userId))) ++ selectAnyPerm)

  def unsetAllPerms(teamId: TeamId): Funit =
    coll.update
      .one(teamQuery(teamId) ++ selectAnyPerm, $unset("perms"), multi = true)
      .void

  def setAllPerms(teamId: TeamId, data: Seq[TeamSecurity.LeaderData]): Funit =
    data.traverse_ { l =>
      coll.update.one(selectId(teamId, l.name), $set("perms" -> l.perms))
    }

  private[team] def countUnsub(teamId: TeamId): Fu[Int] =
    coll.countSel(teamQuery(teamId) ++ $doc("unsub" -> true))

  def teamQuery(teamId: TeamId)                              = $doc("team" -> teamId)
  private def selectId[U: UserIdOf](teamId: TeamId, user: U) = $id(TeamMember.makeId(teamId, user.id))
  private def userQuery(userId: UserId)                      = $doc("user" -> userId)
  private def selectAnyPerm                                  = $doc("perms" $exists true)
