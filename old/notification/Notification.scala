package lila.app
package notification

import user.User

import ornicar.scalalib.Random.nextString

case class Notification(
  id: String,
  user: String,
  html: String,
  from: Option[String])

object Notification {

  def apply(
  user: String,
  html: String,
  from: Option[String]): Notification = new Notification(
    id = nextString(8),
    user = user,
    html = html,
    from = from)
}
