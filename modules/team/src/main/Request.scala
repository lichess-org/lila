package lila.team

import org.joda.time.DateTime

import lila.user.User

case class Request(
    _id: String,
    team: String,
    user: String,
    message: String,
    date: DateTime
) {

  def id = _id
}

object Request {

  def makeId(team: String, user: String) = user + "@" + team

  def make(team: String, user: String, message: String): Request = new Request(
    _id = makeId(team, user),
    user = user,
    team = team,
    message = message.trim,
    date = DateTime.now
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
