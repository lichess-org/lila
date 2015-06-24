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
    case Event(in: In, _) => stay
  }

  when(Enter) {
    case Event(In(data), _) if data contains "Press return to enter the server" =>
      telnet ! BufferUntil(EOM.some)
      send("")
      for (v <- Seq("seek", "shout", "cshout", "pin", "gin")) send(s"set $v 0")
      for (c <- Seq(1, 4, 53)) send(s"- channel $c")
      send("set kiblevel 3000") // shut up if your ELO is < 3000
      send("style 12")
      stay
    case Event(In(data), _) if data contains "Style 12 set." => goto(Throttle)
    case Event(in: In, _)                                    => stay
  }

  when(Ready) {
    case Event(cmd: Command, _) =>
      send(cmd.str)
      goto(Run) using Request(cmd, sender).some
    case Event(Observe(ficsId), _) =>
      send(s"observe $ficsId")
      stay
    case Event(Unobserve(ficsId), _) =>
      send(s"unobserve $ficsId")
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

  def log(lines: List[String]) {
    lines filterNot noise foreach { l =>
      println(s"FICS[$stateName] $l")
    }
    // lines filter noise foreach { l =>
    //   println(s"            (noise) [$stateName] $l")
    // }
  }

  val noiseR = List(
    """^\\ .*""".r,
    """^fics%""".r,
    """^You will not.*""".r,
    """^You are now observing.*""".r,
    """^Game \d+: .*""".r,
    """.*To find more about Relay.*""".r,
    """.*You are already observing game \d+""".r,
    """.*Removing game \d+.*""".r,
    """.*There are no tournaments in progress.*""".r,
    // """.*in the history of both players.*""".r,
    // """.*will be closed in a few minutes.*""".r,
    """^\(told relay\)$""".r,
    """^Game \d+: relay has set .+ clock to .+$""".r, // handle
    """^relay\(.+\)\[\d+\] kibitzes: .*""".r,
    // """Welcome to the Free Internet Chess Server""".r,
    // """Starting FICS session""".r,
    """.*ROBOadmin.*""".r,
    """.*ANNOUNCEMENT.*""".r)

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
  case class Unobserve(ficsId: Int)

  case object Limited {
    val R = "You are already observing the maximum number of games"
    def apply(str: String): Option[Limited.type] = str contains R option (Limited)
  }

  private val EOM = "fics% "
}
