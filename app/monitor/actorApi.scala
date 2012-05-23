package lila
package monitor

import core.CoreEnv

case object GetNbGames
case object GetNbPlaying
case object GetStatus
case object GetMonitorData

case class Update(env: CoreEnv)
