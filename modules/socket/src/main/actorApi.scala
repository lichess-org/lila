package lila.socket
package actorApi

import play.api.libs.json.JsObject

case class Connected(enumerator: JsEnumerator, member: SocketMember)
case class BotConnected(color: chess.Color, v: Boolean)

private[socket] case object Broom
private[socket] case class Quit(uid: Socket.Uid, member: SocketMember)

case class SocketEnter(uid: Socket.Uid, member: SocketMember)
case class SocketLeave(uid: Socket.Uid, member: SocketMember)

case class Resync(uid: Socket.Uid)

case class SendToFlag(flag: String, message: JsObject)

case object PopulationTell
case class NbMembers(nb: Int)
case class RemoteNbMembers(nb: Int)

case class StartWatching(uid: Socket.Uid, member: SocketMember, gameIds: Set[String])
