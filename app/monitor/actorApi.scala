package lila
package monitor

import core.CoreEnv
import socket.SocketMember

case object GetNbGames
case object GetNbPlaying
case object GetStatus
case object GetMonitorData

case class Update(env: CoreEnv)

case class Member(channel: Channel) extends SocketMember {
  val username = none
}

case class Join(uid: String)
case class Connected(channel: Channel)
case class MonitorData(data: List[String])
