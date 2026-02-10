package lila.team

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.perf.UserWithPerfs

case class TeamRequest(
    @Key("_id") id: TeamRequest.ID,
    team: TeamId,
    user: UserId,
    message: String,
    date: Instant,
    declined: Boolean
)

object TeamRequest:

  type ID = String
  def makeId(team: TeamId, user: UserId): ID = s"$user@$team"

  val defaultMessage = "Hello, I would like to join the team!"

  def make(team: TeamId, user: UserId, message: String, declined: Boolean = false): TeamRequest = TeamRequest(
    id = makeId(team, user),
    user = user,
    team = team,
    message = message.trim,
    date = nowInstant,
    declined = declined
  )

case class RequestWithUser(request: TeamRequest, user: UserWithPerfs):
  export request.{ user as _, * }

object RequestWithUser:
  def combine(reqs: List[TeamRequest], users: List[UserWithPerfs]): List[RequestWithUser] = for
    req <- reqs
    user <- users.find(_.user.id == req.user)
  yield RequestWithUser(req, user)

enum Requesting:
  case Joined, NeedRequest, NeedPassword, Blocklist, Closed
