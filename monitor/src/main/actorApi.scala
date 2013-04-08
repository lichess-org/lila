package lila.monitor

case object GetNbGames
case object GetNbMoves
case object GetStatus
case object GetMonitorData

case object Update

case class Join(uid: String)
case class MonitorData(data: List[String])
