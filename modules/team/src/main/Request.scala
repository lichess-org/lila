package lila.team

import org.joda.time.DateTime

import lila.user.User

case class Request(
    _id: String,
    team: TeamId,
    user: User.ID,
    message: String,
    date: DateTime,
    declined: Boolean
):
  inline def id = _id

object Request:

  type ID = String
  def makeId(team: TeamId, user: User.ID) = s"$user@$team"

  val defaultMessage = "Hello, I would like to join the team!"

  def make(team: TeamId, user: User.ID, message: String): Request =
    new Request(
      _id = makeId(team, user),
      user = user,
      team = team,
      message = message.trim,
      date = DateTime.now,
      declined = false
    )

case class RequestWithUser(request: Request, user: User):
  def id      = request.id
  def message = request.message
  def date    = request.date
  def team    = request.team

sealed trait Requesting
object Requesting:
  case object Joined       extends Requesting
  case object NeedRequest  extends Requesting
  case object NeedPassword extends Requesting
