package lila.friend

import lila.user.User

import org.joda.time.DateTime

case class Request(
    id: String,
    user: String,
    friend: String,
    message: String,
    date: DateTime) {
}

object Request {

  def makeId(user: String, friend: String) = user + "@" + friend

  def make(user: String, friend: String, message: String): Request = new Request(
    id = makeId(user, friend),
    user = user,
    friend = friend,
    message = message.trim,
    date = DateTime.now)

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[friend] lazy val tube = Tube[Request](
    __.json update readDate('date) andThen Json.reads[Request],
    Json.writes[Request] andThen (__.json update writeDate('date))
  )
}

case class RequestWithUser(request: Request, user: User) {
  def id = request.id
  def message = request.message
  def date = request.date
}
