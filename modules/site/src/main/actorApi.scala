package lila.site
package actorApi

import lila.socket.SocketMember
import lila.socket.Socket.Uid

case class Member(
  channel: JsChannel,
  userId: Option[String],
  flag: Option[String]) extends SocketMember {

  val troll = false

  def hasFlag(f: String) = flag has f

  def isApi = hasFlag("api")
}

case class Join(uid: Uid, userId: Option[String], flag: Option[String])
private[site] case class Connected(enumerator: JsEnumerator, member: Member)
