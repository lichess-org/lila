package lila.team

import org.joda.time.DateTime

import lila.user.User

private[team] case class Member(
    _id: String,
    team: String,
    user: String,
    date: DateTime
) {

  def is(userId: String): Boolean = user == userId
  def is(user: User): Boolean     = is(user.id)

  def id = _id
}

private[team] object Member {

  def makeId(team: String, user: String) = user + "@" + team

  def make(team: String, user: String): Member =
    new Member(
      _id = makeId(team, user),
      user = user,
      team = team,
      date = DateTime.now
    )
}
