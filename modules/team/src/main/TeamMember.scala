package lila.team

import org.joda.time.DateTime

import lila.common.LightUser
import lila.user.User

private[team] case class TeamMember(
    _id: String,
    team: Team.ID,
    user: User.ID,
    date: DateTime
) {

  def is(userId: String): Boolean = user == userId
  def is(user: User): Boolean     = is(user.id)

  def id = _id
}

object TeamMember {

  case class UserAndDate(user: LightUser, date: DateTime)

  private[team] def makeId(team: String, user: String) = user + "@" + team

  private[team] def make(team: String, user: String): TeamMember =
    new TeamMember(
      _id = makeId(team, user),
      user = user,
      team = team,
      date = DateTime.now
    )
}
