package lila.monitor

import lila.socket.SocketMember

case object GetNbGames
case object GetNbMoves
case object GetStatus
case object GetMonitorData

case object Update

case class Member(channel: JsChannel) extends SocketMember {
  val username = none
}

case class Join(uid: String)
case class Connected(
  enumerator: JsEnumerator, 
  channel: JsChannel)
case class MonitorData(data: List[String])
