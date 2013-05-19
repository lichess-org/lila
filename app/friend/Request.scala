package lila
package friend

import user.User

import com.novus.salat.annotations.Key
import org.joda.time.DateTime

case class Request(
    @Key("_id") id: String,
    user: String,
    friend: String,
    message: String,
    date: DateTime) {
}

object Request {

  def makeId(user: String, friend: String) = user + "@" + friend

  def apply(user: String, friend: String, message: String): Request = new Request(
    id = makeId(user, friend),
    user = user,
    friend = friend,
    message = message.trim,
    date = DateTime.now)
}

case class RequestWithUser(request: Request, user: User) {
  def id = request.id
  def message = request.message
  def date = request.date
}
