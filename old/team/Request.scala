package lila.app
package team

import user.User

import com.novus.salat.annotations.Key
import org.joda.time.DateTime

case class Request(
    @Key("_id") id: String,
    team: String,
    user: String,
    message: String,
    date: DateTime) {
}

object Request {

  def makeId(team: String, user: String) = user + "@" + team

  def apply(team: String, user: String, message: String): Request = new Request(
    id = makeId(team, user),
    user = user,
    team = team,
    message = message.trim,
    date = DateTime.now)
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
