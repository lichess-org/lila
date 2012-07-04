package lila
package notification

import user.User

import ornicar.scalalib.OrnicarRandom.nextString

case class Notification(
  id: String,
  user: User,
  html: String,
  from: Option[User])

object Notification {

  def apply(
  user: User,
  html: String,
  from: Option[User]): Notification = new Notification(
    id = nextString(8),
    user = user,
    html = html,
    from = from)
}
