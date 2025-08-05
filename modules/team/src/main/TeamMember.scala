package lila.team

import lila.core.LightUser

case class TeamMember(
    _id: String,
    team: TeamId,
    user: UserId,
    date: Instant,
    perms: Set[TeamSecurity.Permission] = Set.empty
):
  inline def id = _id

  def hasPerm(perm: TeamSecurity.Permission.Selector): Boolean =
    perms(perm(TeamSecurity.Permission))

object TeamMember:

  given UserIdOf[TeamMember] = _.user

  case class UserAndDate(user: LightUser, date: Instant)

  private[team] def makeId[U: UserIdOf](team: TeamId, user: U) = s"${user.id}@$team"

  private[team] def make(team: TeamId, user: UserId): TeamMember = new TeamMember(
    _id = makeId(team, user),
    user = user,
    team = team,
    date = nowInstant
  )

  private[team] def parseId(id: String): Option[(UserId, TeamId)] = id.split('@') match
    case Array(userId, teamId) => (UserId(userId), TeamId(teamId)).some
    case _ => None
