package lila.team

import org.joda.time.DateTime

import lila.user.User

case class Request(
    id: String,
    team: String,
    user: String,
    message: String,
    date: DateTime) {
}

object Request {

  def makeId(team: String, user: String) = user + "@" + team

  def make(team: String, user: String, message: String): Request = new Request(
    id = makeId(team, user),
    user = user,
    team = team,
    message = message.trim,
    date = DateTime.now)

  import lila.db.JsTube, JsTube.Helpers._
  import play.api.libs.json._

  private[team] lazy val tube = JsTube(
    (__.json update readDate('date)) andThen Json.reads[Request],
    Json.writes[Request] andThen (__.json update writeDate('date))
  ) 
}

case class RequestWithUser(request: Request, user: User) {
  def id = request.id
  def message = request.message
  def date = request.date
  def team = request.team
}

sealed trait Requesting
case class Joined(team: Team) extends Requesting
case class Motivate(team: Team) extends Requesting
