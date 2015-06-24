package lila.relay
package actorApi

import lila.socket.SocketMember

private[relay] case class Member(
  channel: JsChannel,
  userId: Option[String],
  troll: Boolean) extends SocketMember

private[relay] object Member {
  def apply(channel: JsChannel, user: Option[lila.user.User]): Member = Member(
    channel = channel,
    userId = user map (_.id),
    troll = user.??(_.troll))
}

private[relay] case class Messadata(trollish: Boolean = false)

private[relay] case class Join(
  uid: String,
  user: Option[lila.user.User],
  version: Int)
private[relay] case class Talk(tourId: String, u: String, t: String, troll: Boolean)
private[relay] case class Connected(enumerator: JsEnumerator, member: Member)
private[relay] case object Reload

private[relay] case object NotifyCrowd
