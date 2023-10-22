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

  def make(team: TeamId, user: UserId, message: String, declined: Boolean = false): Request = Request(
    _id = makeId(team, user),
    user = user,
    team = team,
    message = message.trim,
    date = nowInstant,
    declined = declined
  )

case class RequestWithUser(request: Request, user: User.WithPerfs):
  export request.{ user as _, * }

object RequestWithUser:
  def combine(reqs: List[Request], users: List[User.WithPerfs]): List[RequestWithUser] = for
    req  <- reqs
    user <- users.find(_.user.id == req.user)
  yield RequestWithUser(req, user)

enum Requesting:
  case Joined, NeedRequest, NeedPassword, Blocklist
