package lila.relay

sealed trait GameEvent {
  val ficsId: Int
}
object GameEvent {
  case class Move(ficsId: Int, san: String, ply: Int, white: String, black: String) extends GameEvent {
    override def toString = s"[$ficsId] $ply: $san"
  }
  object Move {
    def apply(str: String): Option[Move] = {
      val split = str split ' '
      for {
        ficsId <- split lift 16 flatMap parseIntOption
        san <- split lift 29
        white <- split lift 17
        black <- split lift 18
        turn <- split lift 26 flatMap parseIntOption
        color <- split lift 9 map { x => chess.Color(x == "W") }
        ply = (turn - 1) * 2 + color.fold(0, 1)
      } yield Move(ficsId, san, ply, white, black)
    }
  }

  case class Resign(ficsId: Int, loser: String) extends GameEvent
  case object Resign {
    val R = """(?i)^relay\(.+\)\[(\d+)\] kibitzes: (\w+) has resigned.+$""".r
    def apply(str: String): Option[Resign] = str match {
      case R(id, name) => parseIntOption(id) map { Resign(_, name) }
      case _           => none
    }
  }

  case class Draw(ficsId: Int) extends GameEvent
  case object Draw {
    val R = """(?i)^relay\(.+\)\[(\d+)\] kibitzes: The game is officially a draw.+$""".r
    def apply(str: String): Option[Draw] = str match {
      case R(id) => parseIntOption(id) map { Draw(_) }
      case _     => none
    }
  }

  case class Clock(ficsId: Int, player: String, tenths: Int) extends GameEvent
  case object Clock {
    val R = """^Game (\d+): relay has set (\w+)'s clock to ([0-9:\.]+)\.$""".r
    def apply(str: String): Option[Clock] = str match {
      case R(id, player, clock) => for {
        ficsId <- parseIntOption(id)
        tenths <- toTenths(clock)
      } yield Clock(ficsId, player, tenths)
      case _ => none
    }
    // Game 377: relay has set FMIvanBocharov's clock to 0:53:35.
    def toTenths(clock: String): Option[Int] =
      clock.split(":").flatMap(parseIntOption) match {
        case Array(hours, minutes, seconds) => Some((60 * 60 * hours + 60 * minutes + seconds) * 10)
        case _ =>
          println(s"[relay] invalid player clock $clock")
          none
      }
  }
}
