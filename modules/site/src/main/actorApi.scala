package lidraughts.site
package actorApi

import lidraughts.socket.SocketMember
import lidraughts.socket.Socket.Uid

case class Member(
    channel: JsChannel,
    userId: Option[String],
    flag: Option[String]
) extends SocketMember {

  val troll = false

  def isApi = flag has "api"
}

case class Join(uid: Uid, userId: Option[String], flag: Option[String])
private[site] case class Connected(enumerator: JsEnumerator, member: Member)
