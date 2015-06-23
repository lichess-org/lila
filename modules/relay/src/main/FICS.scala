package lila.relay

import akka.actor._
import akka.pattern.pipe
import scala.concurrent.duration._

import lila.hub.actorApi.map.Tell

private[relay] final class FICS(
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
      val lines = handleMovesAndReturnOtherLines(in)
      cmd parse lines match {
        case Some(res) =>
          replyTo ! res
          goto(Ready) using none
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

  whenUnhandled {
    case Event(_: Command, _) =>
      stash()
      stay
    case Event(_: Observe, _) =>
      stash()
      stay
    case Event(in: In, _) =>
      log(handleMovesAndReturnOtherLines(in))
      stay
  }

  onTransition {
    case x -> Ready =>
      send("")
      unstashAll()
    // case x -> y =>
    //   println(x, y)
  }

  def handleMovesAndReturnOtherLines(in: In): List[String] = {
    val (moves, others) = in.lines.partition(_ startsWith "<12>")
    moves.foreach { l =>
      val move = Move parse l err s"Unparsable FICS move line $l"
      context.system.lilaBus.publish(move, 'relayMove)
    }
    others
  }

  def log(data: String) {
    if (data.nonEmpty && !noise(data)) println(s"FICS[$stateName] $data")
  }

  def log(lines: List[String]) {
    log(lines filterNot ("fics%"==) mkString "\n")
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

  case class Move(ficsId: Int, san: String, ply: Int, log: String) {
    override def toString = s"[$ficsId] $ply: $san from ${log split ' ' drop 9 mkString " "}"
  }
  object Move {
    def parse(str: String): Option[Move] = {
      val split = str split ' '
      for {
        ficsId <- split lift 16 flatMap parseIntOption
        san <- split lift 29
        turn <- split lift 26 flatMap parseIntOption
        color <- split lift 9 map { x => chess.Color(x == "W") }
        ply = (turn - 1) * 2 + color.fold(0, 1)
      } yield Move(ficsId, san, ply, str)
    }
  }

  private val EOM = "fics% "
}
