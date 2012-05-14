package lila
package report

case object GetNbGames
case object GetNbPlaying
case object GetStatus

case class Update(env: SystemEnv)
