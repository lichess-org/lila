package lila.socket
package actorApi

import play.api.libs.json.JsObject

case class Connected(enumerator: JsEnumerator, member: SocketMember)
case class BotConnected(color: chess.Color, v: Boolean)

private[socket] case object Broom
private[socket] case class Quit(sri: Socket.Sri, member: SocketMember)

case class SocketEnter(sri: Socket.Sri, member: SocketMember)
case class SocketLeave(sri: Socket.Sri, member: SocketMember)

case class Resync(sri: Socket.Sri)

case class SendToFlag(flag: String, message: JsObject)

case object PopulationTell
case class NbMembers(nb: Int)
case class RemoteNbMembers(nb: Int)

case class StartWatching(sri: Socket.Sri, member: SocketMember, gameIds: Set[String])
