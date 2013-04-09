package lila.monitor

import lila.socket._

case class Member(channel: JsChannel) extends SocketMember {
  val userId = none
}

case object GetNbGames
case object GetNbMoves
case object GetStatus
case object GetMonitorData

case object Update

case class Join(uid: String)
case class MonitorData(data: List[String])
