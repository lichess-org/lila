package lila.relay

import akka.actor.{ Props, Actor, ActorRef, Status, FSM => AkkaFSM }
import akka.pattern.pipe
import scala.concurrent.Await
import scala.concurrent.duration._

import actorApi._
import lila.game.Game

private[relay] final class RelayFSM(importer: Importer) extends Actor with AkkaFSM[State, Option[String]] {

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
      goto(Configure)
  }

  when(Configure) {
    case Event(In(str), _) if str endsWith "fics% " =>
      for (v <- Seq("seek", "shout", "cshout", "kibitz", "pin", "gin")) send(s"set $v 0")
      for (c <- Seq(4, 53)) send(s"- channel $c")
      send("style 12")
      goto(Lobby)
  }

  when(Lobby) {
    case Event(In(str), _) if str endsWith "fics% " =>
      send("unobserve")
      send("observe /l")
      // wait so other messages pass and the movelist is full
      context.system.scheduler.scheduleOnce(700 millis) { send("moves") }
      goto(Create)
  }

  when(Create, stateTimeout = 7 second) {
    case Event(In(str), _) if (str.contains("Movelist for game ") && str.contains("{Still in progress}")) =>
      val data = Parser game str err s"Can't parse game from $str"
      val gameId = Await.result(
        importer create data,
        8 seconds).id
      log("Start relaying game " + gameId)
      println(s"http://en.l.org/${gameId}")
      goto(Observe) using gameId.some
    case Event(In(str), _) if (str.contains("Movelist for game ") || str.contains("{Still in progress}")) =>
      log("Received truncated move list, waiting for next game")
      stay
    case Event(StateTimeout, _) =>
      log("state timeout")
      goto(Lobby) using none
    case Event(In(str), _) if (str contains "Removing game ") =>
      log(str)
      goto(Lobby) using none
  }

  when(Observe) {
    case Event(In(str), Some(gameId)) if str contains "<12>" =>
      importer.move(gameId, Parser move str) onFailure {
        case e: Exception => self ! MoveFail(e)
      }
      stay
    case Event(MoveFail(e), _) =>
      log(s"Move fail: ${e.getMessage}")
      goto(Lobby) using none
    case Event(In(str), _) if (str contains "Removing game ") => goto(Lobby) using none
  }

  whenUnhandled {
    case Event(In(str), _) =>
      log(str)
      stay
  }

  onTransition {
    case x -> Lobby => send("")
  }

  def log(msg: String) {
    if (!noise(msg)) println(s"FICS[$stateName] $msg")
  }

  val noiseR = List(
    """(?s).*Welcome to the Free Internet Chess Server.*""".r,
    // """^\n[a-zA-z]+(\([^\)]+\)){1,2}:\s.+\nfics\%\s$""".r, // people chating
    """(?s).*Starting FICS session.*""".r,
    """(?s).*ROBOadmin.*""".r)

  def noise(str: String) = noiseR exists matches(str)

  def matches(str: String)(r: scala.util.matching.Regex) = r.pattern.matcher(str).matches
}
