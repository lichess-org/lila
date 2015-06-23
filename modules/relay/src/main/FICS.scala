package lila.relay

import akka.actor._
import akka.pattern.pipe
import scala.concurrent.duration._

import lila.hub.actorApi.map.Tell

private[relay] final class FICS(
    actorMap: ActorRef,
    remote: java.net.InetSocketAddress) extends Actor with Stash with LoggingFSM[FICS.State, Option[FICS.Request]] {

  import FICS._
  import Telnet._
  import command.Command

  var send: String => Unit = _

  val telnet = context.actorOf(Props(classOf[Telnet], remote, self), name = "telnet")

  startWith(Connect, none)

  when(Connect) {
    case Event(Connection(s), _) =>
      send = s
      goto(Login)
  }

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
      for (c <- Seq(4, 53)) send(s"- channel $c")
      send("set kiblevel 3000") // shut up if your ELO is < 3000
      send("style 12")
      goto(Ready)
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
      cmd parse in.lines match {
        case Some(res) =>
          replyTo ! res
          goto(Ready) using none
        case None =>
          log(in.data)
          stay
      }
    case Event(StateTimeout, req) =>
      log("state timeout")
      req.foreach { r =>
        r.replyTo ! Status.Failure(new Exception(s"FICS:Run timeout on ${r.cmd.str}"))
      }
      goto(Ready) using none
  }

  whenUnhandled {
    case Event(_: Command, _) =>
      stash()
      stay
    case Event(_: Observe, _) =>
      stash()
      stay
    case Event(in: In, _) if in.data contains "<12>" =>
      in.lines.filter(_ startsWith "<12>") foreach { line =>
        val splitted = line split ' '
        for {
          ficsId <- splitted lift 16
          move <- splitted lift 29
        } actorMap ! Tell(ficsId, GameActor.Move(move))
      }
      stay
    case Event(In(data), _) =>
      log(data)
      stay
  }

  onTransition {
    case x -> Ready =>
      send("")
      unstashAll()
    // case x -> y =>
    //   println(x, y)
  }

  def log(msg: String) {
    if (!noise(msg)) println(s"FICS[$stateName] $msg")
  }

  val noiseR = List(
    // """(?is)^\n\w+(\([^\)]+\)){1,2}:\s.+\nfics\%\s$""".r, // people chating
    // """(?is)^\n\w+(\([^\)]+\)){1,2}\[\d+\]\s.+\nfics\%\s$""".r, // people whispering
    """(?s).*Welcome to the Free Internet Chess Server.*""".r,
    """(?s).*Starting FICS session.*""".r,
    """(?s).*ROBOadmin.*""".r,
    """ANNOUNCEMENT""".r)

  def noise(str: String) = noiseR exists matches(str)

  def matches(str: String)(r: scala.util.matching.Regex) = r.pattern.matcher(str).matches
}

object FICS {

  sealed trait State
  case object Connect extends State
  case object Login extends State
  case object Enter extends State
  case object Configure extends State
  case object Ready extends State
  case object Run extends State

  case class Request(cmd: command.Command, replyTo: ActorRef)

  case class Observe(ficsId: Int)

  private val EOM = "fics% "
}
