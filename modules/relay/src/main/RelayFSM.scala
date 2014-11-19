package lila.relay

import akka.actor.{ Props, Actor, ActorRef, Status, FSM => AkkaFSM }
import scala.concurrent.duration._

import actorApi._
import lila.game.Game

private[relay] final class RelayFSM(
    importer: lila.importer.Importer,
    liveImporter: lila.importer.Live,
    userId: String,
    importIP: String) extends Actor with AkkaFSM[State, Option[Game]] {

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
      for (v <- Seq("seek", "shout", "cshout", "kibitz", "pin", "gin"))
        send(s"set $v 0")
      for (c <- Seq(4, 53)) send(s"- channel $c")
      send("style 12")
      send("observe /b")
      send("moves")
      goto(Create)
  }

  when(Create) {
    case Event(In(str), _) if str contains "Movelist for game " =>
      val pgn = Parser pgn str
      println(s"str##$str##")
      println(s"pgn##$pgn##")
      val game = scala.concurrent.Await.result(
        importer.doImport(pgn, userId.some, importIP),
        10 seconds).pp
      goto(Observe) using game.some
  }

  when(Observe) {
    case Event(In(str), Some(game)) if str contains "<12>" =>
      log(str)
      stay
  }

  whenUnhandled {
    case Event(In(str), _) =>
      log(str)
      stay
  }

  def log(msg: String) {
    if (!noise(msg)) println(s"FICS<$stateName $msg")
  }

  val noiseR = List(
    """(?s).*Welcome to the Free Internet Chess Server.*""".r,
    // """^\n[a-zA-z]+(\([^\)]+\)){1,2}:\s.+\nfics\%\s$""".r, // people chating
    """(?s).*Starting FICS session.*""".r,
    """(?s).*ROBOadmin.*""".r)

  def noise(str: String) = noiseR exists matches(str)

  def matches(str: String)(r: scala.util.matching.Regex) = r.pattern.matcher(str).matches
}
