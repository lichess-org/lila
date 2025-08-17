package lila.team

import reactivemongo.api.bson.*
import reactivemongo.api.commands.WriteResult

import lila.db.dsl.{ *, given }
import lila.team.TeamSecurity.Permission

final class TeamMemberRepo(val coll: Coll)(using Executor):

  import BSONHandlers.given

  // expensive with thousands of members!
  def userIdsByTeam(teamId: TeamId): Fu[List[UserId]] =
    coll.secondary.distinctEasy[UserId, List]("user", $doc("team" -> teamId))

  def removeByTeam(teamId: TeamId): Funit =
    coll.delete.one(teamQuery(teamId)).void

  def removeByUser(userId: UserId): Funit =
    coll.delete.one(selectUser(userId)).void

  def get[U: UserIdOf](teamId: TeamId, userId: U): Fu[Option[TeamMember]] =
    coll.byId[TeamMember](TeamMember.makeId(teamId, userId))

  def exists[U: UserIdOf](teamId: TeamId, user: U): Fu[Boolean] =
    coll.exists(selectId(teamId, user))

  def add(teamId: TeamId, userId: UserId, perms: Set[TeamSecurity.Permission] = Set.empty): Funit =
    coll.insert.one(TeamMember.make(team = teamId, user = userId).copy(perms = perms)).void

  def remove(teamId: TeamId, userId: UserId): Fu[WriteResult] =
    coll.delete.one(selectId(teamId, userId))

  def countByTeam(teamId: TeamId): Fu[Int] =
    coll.countSel(teamQuery(teamId))

  private[team] def filterUserIdsInTeam[U: UserIdOf](teamId: TeamId, users: Iterable[U]): Fu[Set[UserId]] =
    users.nonEmpty.so(
      coll.distinctEasy[UserId, Set]("user", $inIds(users.map { TeamMember.makeId(teamId, _) }))
    )

  def isSubscribed[U: UserIdOf](team: Team, user: U): Fu[Boolean] =
    coll.exists(selectId(team.id, user) ++ $doc("unsub" -> true)).not

  def subscribe(teamId: TeamId, userId: UserId, v: Boolean): Funit =
    coll.update
      .one(
        selectId(teamId, userId),
        if v then $unset("unsub")
        else $set("unsub" -> true)
      )
      .void

  def hasPerm[A: UserIdOf](teamId: TeamId, user: A, perm: Permission.Selector): Fu[Boolean] =
    coll.exists(selectId(teamId, user) ++ $doc("perms" -> perm(Permission)))

  def hasAnyPerm[A: UserIdOf](teamId: TeamId, user: A): Fu[Boolean] =
    coll.exists(selectId(teamId, user) ++ selectAnyPerm)

  def setPerms[A: UserIdOf](teamId: TeamId, user: A, perms: Set[Permission]): Funit =
    coll
      .updateOrUnsetField(selectId(teamId, user.id), "perms", perms.nonEmpty.option(perms))
      .void

  def leaders(teamId: TeamId, perm: Option[Permission.Selector] = None): Fu[List[TeamMember]] =
    coll
      .find(teamQuery(teamId) ++ perm.fold(selectAnyPerm)(selectPerm))
      .sort($doc("date" -> 1))
      .cursor[TeamMember]()
      .listAll()

  def leadersOf[U: UserIdOf](user: U, perm: Permission.Selector): Fu[List[TeamMember]] =
    coll.list[TeamMember](selectUser(user) ++ selectPerm(perm))

  private[team] def leaderIds(teamId: TeamId): Fu[Set[UserId]] =
    coll.primitive[UserId](teamQuery(teamId) ++ selectAnyPerm, "user").dmap(_.toSet)

  def publicLeaderIds(teamIds: Seq[TeamId]): Fu[List[UserId]] =
    coll.primitive[UserId](
      teamQuery(teamIds) ++ $doc("perms" -> Permission.Public),
      "user"
    )

  def leadsOneOf(userId: UserId, teamIds: Seq[TeamId]): Fu[Boolean] =
    teamIds.nonEmpty.so(coll.secondary.exists(selectIds(teamIds, userId) ++ selectAnyPerm))

  def teamsLedBy[U: UserIdOf](leader: U, perm: Option[Permission.Selector]): Fu[Seq[TeamId]] =
    coll.secondary
      .primitive[TeamId](selectUser(leader) ++ perm.fold(selectAnyPerm)(selectPerm), "team")

  def filterLedBy(teamIds: Seq[TeamId], leader: UserId): Fu[Set[TeamId]] =
    coll.secondary
      .primitive[TeamId](selectIds(teamIds, leader) ++ selectAnyPerm, "team")
      .dmap(_.toSet)

  def teamsWhereIsGrantedRequest(leader: UserId): Fu[List[TeamId]] =
    coll.distinctEasy[TeamId, List](
      "team",
      $doc("user" -> leader, "perms" -> TeamSecurity.Permission.Request),
      _.sec
    )

  def unsetAllPerms(teamId: TeamId): Funit =
    coll.update
      .one(teamQuery(teamId) ++ selectAnyPerm, $unset("perms"), multi = true)
      .void

  def setAllPerms(teamId: TeamId, data: List[TeamSecurity.LeaderData]): Funit =
    data.sequentiallyVoid: l =>
      setPerms(teamId, l.name, l.perms)

  def addPublicLeaderIds(teams: Seq[Team]): Fu[List[Team.WithPublicLeaderIds]] =
    coll
      .primitive[String](
        teamQuery(teams.map(_.id)) ++ $doc("perms" -> Permission.Public),
        "_id"
      )
      .map:
        _.flatMap(TeamMember.parseId).groupBy(_._2).view.mapValues(_.map(_._1)).toMap
      .map: grouped =>
        teams.view.map(t => Team.WithPublicLeaderIds(t, grouped.getOrElse(t.id, Nil))).toList

  def addPublicLeaderIds(team: Team): Fu[Team.WithPublicLeaderIds] =
    coll
      .primitive[String](
        teamQuery(team.id) ++ $doc("perms" -> Permission.Public),
        "_id"
      )
      .map:
        _.flatMap(TeamMember.parseId).view.map(_._1).toList
      .map(Team.WithPublicLeaderIds(team, _))

  def addMyLeadership(teams: Seq[Team])(using me: Option[MyId]): Fu[List[Team.WithMyLeadership]] =
    me.so(filterLedBy(teams.map(_.id), _))
      .map: myTeams =>
        teams.view.map(t => Team.WithMyLeadership(t, myTeams contains t.id)).toList

  private[team] def countUnsub(teamId: TeamId): Fu[Int] =
    coll.countSel(teamQuery(teamId) ++ $doc("unsub" -> true))

  def teamQuery(teamId: TeamId) = $doc("team" -> teamId)
  def teamQuery(teamIds: Seq[TeamId]) = $doc("team".$in(teamIds))
  private def selectId[U: UserIdOf](teamId: TeamId, user: U) = $id(TeamMember.makeId(teamId, user.id))
  private def selectIds[U: UserIdOf](teamIds: Seq[TeamId], user: U) = $inIds:
    teamIds.map(TeamMember.makeId(_, user.id))
  private def selectUser[U: UserIdOf](user: U) = $doc("user" -> user.id)
  private def selectAnyPerm = $doc("perms".$exists(true))
  private def selectPerm(perm: Permission.Selector) = $doc("perms" -> perm(Permission))
