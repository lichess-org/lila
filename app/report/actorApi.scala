package lila
package report

import core.CoreEnv

case object GetNbGames
case object GetNbPlaying
case object GetStatus

case class Update(env: CoreEnv)
