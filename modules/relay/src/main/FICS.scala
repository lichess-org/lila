package lila.relay

import akka.actor._
import akka.pattern.pipe
import scala.concurrent.duration._

import lila.hub.actorApi.map.Tell

private[relay] final class FICS(config: FICS.Config) extends Actor with Stash with LoggingFSM[FICS.State, Option[FICS.Request]] {

  import FICS._
  import Telnet._
  import GameEvent._
  import command.Command

  var send: String => Unit = _

  val telnet = context.actorOf(Props(classOf[Telnet], config.remote, self), name = "telnet")

  startWith(Connect, none)

  when(Connect) {
    case Event(Connection(s), _) =>
      send = s
      goto(Login)
  }

  //   when(Login) {
  //     case Event(In(data), _) if data endsWith "login: " =>
  //       send(config.login)
  //       stay
  //     case Event(In(data), _) if data endsWith "password: " =>
  //       send(config.password)
  //       telnet ! BufferUntil(EOM.some)
  //       goto(Configure)
  //   }

  when(Login) {
    case Event(In(data), _) if data endsWith "login: " =>
      send("guest")
      goto(Enter)
  }

  when(Enter) {
    case Event(In(data), _) if data contains "Press return to enter the server" =>
      telnet ! BufferUntil(EOM.some)
      send("")
      goto(Configure)
  }

  when(Configure) {
    case Event(In(_), _) =>
      for (v <- Seq("seek", "shout", "cshout", "pin", "gin")) send(s"set $v 0")
      for (c <- Seq(1, 4, 53)) send(s"- channel $c")
      send("set kiblevel 3000") // shut up if your ELO is < 3000
      send("style 12")
      goto(Throttle)
  }

  when(Ready) {
    case Event(cmd: Command, _) =>
      send(cmd.str)
      goto(Run) using Request(cmd, sender).some
    case Event(Observe(ficsId), _) =>
      send(s"observe $ficsId")
      stay
  }

  when(Run, stateTimeout = 7 second) {
    case Event(in: In, Some(Request(cmd, replyTo))) =>
      val lines = handle(in)
      cmd parse lines match {
        case Some(res) =>
          replyTo ! res
          goto(Throttle) using none
        case None =>
          log(lines)
          stay
      }
    case Event(StateTimeout, req) =>
      log("state timeout")
      req.foreach { r =>
        r.replyTo ! Status.Failure(new Exception(s"FICS:Run timeout on ${r.cmd.str}"))
      }
      goto(Ready) using none
  }

  when(Throttle, stateTimeout = 500 millis) {
    case Event(StateTimeout, _) => goto(Ready) using none
  }

  whenUnhandled {
    case Event(_: Command, _) =>
      stash()
      stay
    case Event(_: Observe, _) =>
      stash()
      stay
    case Event(in: In, _) =>
      log(handle(in))
      stay
  }

  onTransition {
    case _ -> Ready => unstashAll()
  }

  def handle(in: In): List[String] = in.lines.foldLeft(List.empty[String]) {
    case (lines, line) =>
      Move(line) orElse Resign(line) orElse Draw(line) orElse Limited(line) map {
        case move: Move =>
          context.parent ! move
          lines
        case resign: Resign =>
          context.parent ! resign
          lines
        case draw: Draw =>
          context.parent ! draw
          lines
        case Limited =>
          println(s"FICS ERR $line")
          lines
      } getOrElse {
        line :: lines
      }
  }.reverse

  def log(data: String) {
    if (data.nonEmpty && !noise(data))
      println(s"FICS[$stateName] ${data.lines.filter(_.nonEmpty).mkString("\n")}")
  }

  def log(lines: List[String]) {
    log(lines filterNot ("fics%"==) mkString "\n")
  }

  val noiseR = List(
    // """(?is)^\n\w+(\([^\)]+\)){1,2}:\s.+\nfics\%\s$""".r, // people chating
    // """(?is)^\n\w+(\([^\)]+\)){1,2}\[\d+\]\s.+\nfics\%\s$""".r, // people whispering
    """^\(told relay\)$""".r,
    """^Game \d+: relay has set .+ clock to .+""".r,
    """^relay\(.+\)\[\d+\] kibitzes: .+""".r,
    """(?s).*Welcome to the Free Internet Chess Server.*""".r,
    """(?s).*Starting FICS session.*""".r,
    """(?s).*ROBOadmin.*""".r,
    """ANNOUNCEMENT""".r)

  def noise(str: String) = noiseR exists matches(str)

  def matches(str: String)(r: scala.util.matching.Regex) = r.pattern.matcher(str).matches
}

object FICS {

  case class Config(host: String, port: Int, login: String, password: String, enabled: Boolean) {
    def remote = new java.net.InetSocketAddress(host, port)
  }

  sealed trait State
  case object Connect extends State
  case object Login extends State
  case object Enter extends State
  case object Configure extends State
  case object Ready extends State
  case object Run extends State
  case object Throttle extends State

  case class Request(cmd: command.Command, replyTo: ActorRef)

  case class Observe(ficsId: Int)

  case object Limited {
    val R = "You are already observing the maximum number of games"
    def apply(str: String): Option[Limited.type] = str contains R option(Limited)
  }

  private val EOM = "fics% "
}
