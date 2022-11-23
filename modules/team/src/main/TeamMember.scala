package lila.team

import org.joda.time.DateTime

import lila.common.LightUser
import lila.user.User

private[team] case class TeamMember(
    _id: String,
    team: TeamId,
    user: User.ID,
    date: DateTime
):
  inline def id = _id

  def is(userId: User.ID): Boolean = user == userId
  def is(user: User): Boolean      = is(user.id)

object TeamMember:

  case class UserAndDate(user: LightUser, date: DateTime)

  private[team] def makeId(team: TeamId, user: String) = user + "@" + team

  private[team] def make(team: TeamId, user: String): TeamMember = new TeamMember(
    _id = makeId(team, user),
    user = user,
    team = team,
    date = DateTime.now
  )
