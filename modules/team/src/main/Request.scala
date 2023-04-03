package lila.team

import lila.user.User

case class Request(
    _id: String,
    team: TeamId,
    user: UserId,
    message: String,
    date: Instant,
    declined: Boolean
):
  inline def id = _id

object Request:

  type ID = String
  def makeId(team: TeamId, user: UserId) = s"$user@$team"

  val defaultMessage = "Hello, I would like to join the team!"

  def make(team: TeamId, user: UserId, message: String): Request =
    new Request(
      _id = makeId(team, user),
      user = user,
      team = team,
      message = message.trim,
      date = nowInstant,
      declined = false
    )

case class RequestWithUser(request: Request, user: User):
  def id      = request.id
  def message = request.message
  def date    = request.date
  def team    = request.team

enum Requesting:
  case Joined, NeedRequest, NeedPassword
