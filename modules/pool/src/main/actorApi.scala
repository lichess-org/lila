package lila.pool
package actorApi

import lila.game.Game
import lila.socket.SocketMember
import lila.user.User

case class Member(
  channel: JsChannel,
  userId: Option[String],
  troll: Boolean) extends SocketMember

object Member {
  def apply(channel: JsChannel, user: Option[User]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    troll = user.??(_.troll))
}

case class Join(
  uid: String,
  user: Option[User],
  version: Int)

case class Talk(tourId: String, u: String, t: String, troll: Boolean)
