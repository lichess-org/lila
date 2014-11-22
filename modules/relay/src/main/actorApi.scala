package lila.relay
package actorApi

// states
sealed trait State
case object Connect extends State
case object Login extends State
case object Enter extends State
case object Configure extends State
case object Lobby extends State
case object Create extends State
case object Observe extends State
case object Run extends State

case class MoveFail(e: Exception)
