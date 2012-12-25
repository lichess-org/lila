package lila
package team

import com.novus.salat.annotations.Key
import org.joda.time.DateTime

import user.User

case class Member(
    @Key("_id") id: String, 
    team: String,
    user: String,
    date: DateTime) {

  def is(userId: String): Boolean = user == userId
  def is(user: User): Boolean = is(user.id)
}

object Member {

  def makeId(team: String, user: String) = user + "@" + team

  def apply(team: String, user: String): Member = new Member(
    id = makeId(team, user),
    user = user, 
    team = team, 
    date = DateTime.now)
}

case class MemberWithUser(member: Member, user: User) {
  def team = member.team
  def date = member.date
}
