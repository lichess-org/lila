package lila.team

import org.joda.time.DateTime

import lila.user.User

private[team] case class Member(
    id: String, 
    team: String,
    user: String,
    date: DateTime) {

  def is(userId: String): Boolean = user == userId
  def is(user: User): Boolean = is(user.id)
}

private[team] object Member {

  def makeId(team: String, user: String) = user + "@" + team

  def make(team: String, user: String): Member = new Member(
    id = makeId(team, user),
    user = user, 
    team = team, 
    date = DateTime.now)

  import lila.db.Tube, Tube.Helpers._
  import play.api.libs.json._

  private[team] lazy val tube = Tube(
    reader = (__.json update readDate('date)) andThen Json.reads[Member],
    writer = Json.writes[Member],
    writeTransformer = (__.json update writeDate('date)).some
  ) 
}

private[team] case class MemberWithUser(member: Member, user: User) {
  def team = member.team
  def date = member.date
}
