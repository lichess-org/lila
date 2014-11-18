package lila.relay

import akka.actor.{ Props, Actor, ActorRef, Status, FSM => AkkaFSM }

import actorApi._
import lila.game.Game

private[relay] final class RelayFSM(
  importer: lila.importer.Live,
  userId: String) extends Actor with AkkaFSM[State, Option[Game]] {

  import Telnet._

  var send: String => Unit = _

  startWith(Connect, none)

  when(Connect) {
    case Event(Connection(s), _) =>
      send = s
      goto(Login)
  }

  when(Login) {
    case Event(In(str), _) if str endsWith "login: " =>
      send("guest")
      goto(Enter)
  }

  when(Enter) {
    case Event(In(str), _) if str contains "Press return to enter the server" =>
      send("")
      goto(Lobby)
  }

  when(Lobby) {
    case Event(In(str), _) if str endsWith "fics% " =>
      send("style 12")
      send("observe /l")
      send("moves")
      goto(Create)
  }

  when(Create) {
    case Event(In(str), _) if str contains "Movelist for game " =>
      log(str)
      stay
  }

  when(Observe) {
    case Event(In(str), _) if str contains "<12>" =>
      log(str)
      stay
  }

  whenUnhandled {
    case Event(In(str), _) =>
      log(str)
      stay
  }

  def log(msg: String) {
    if (!noise(msg)) println(s"*$stateName $msg*")
  }

  val noiseR = List(
    """(?s).*\("play \d+" to respond\).*""".r,
    """(?s).*Starting FICS session.*""".r,
    """(?s).*ROBOadmin.*""".r)

  def noise(str: String) = noiseR exists matches(str)

  def matches(str: String)(r: scala.util.matching.Regex) = r.pattern.matcher(str).matches
}
