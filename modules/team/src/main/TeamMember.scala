package lila.team

import lila.common.LightUser
import lila.user.User

import cats.derived.*

case class TeamMember(
    _id: String,
    team: TeamId,
    user: UserId,
    date: Instant,
    perms: Set[TeamMember.Permission] = Set.empty
):
  inline def id = _id

  def is(userId: UserId): Boolean = user == userId
  def is(user: User): Boolean     = is(user.id)

object TeamMember:

  case class UserAndDate(user: LightUser, date: Instant)

  enum Permission(val desc: String) derives Eq:
    case Public   extends Permission("Visible as leader on the team page")
    case Settings extends Permission("Manage the team settings")
    case Tour     extends Permission("Create, manage and join team tournaments")
    case Comm     extends Permission("Moderate the forum and chats")
    case Accept   extends Permission("Accept and decline join requests")
    case Kick     extends Permission("Kick members of the team")
    case Admin    extends Permission("Manage leader permissions")
    def key = toString.toLowerCase
  object Permission:
    type Selector = Permission.type => Permission
    def byKey(key: String): Option[Permission] = values.find(_.key == key)

  private[team] def makeId[U: UserIdOf](team: TeamId, user: U) = s"${user.id}@$team"

  private[team] def make(team: TeamId, user: UserId): TeamMember = new TeamMember(
    _id = makeId(team, user),
    user = user,
    team = team,
    date = nowInstant
  )
