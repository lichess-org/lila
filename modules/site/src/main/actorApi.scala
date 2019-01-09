package lila.site
package actorApi

import scala.concurrent.Promise

import lila.socket.SocketMember
import lila.socket.Socket.Uid

case class Member(
    channel: JsChannel,
    userId: Option[String],
    flag: Option[String]
) extends SocketMember {

  def isApi = flag has "api"
}

private[site] case class Join(uid: Uid, userId: Option[String], flag: Option[String], promise: Promise[Connected])
private[site] case class Connected(enumerator: JsEnumerator, member: Member)
