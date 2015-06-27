package lila.relay
package command

import org.joda.time.DateTime
import scala.util.matching.Regex
import scala.util.{ Try, Success, Failure }

sealed trait Command extends FICS.Stashable {
  type Result
  def str: String
  def parse(lines: List[String]): Option[Result]
}

case object ListTourney extends Command {
  type Result = List[Tourney]
  val str = "tell relay listtourney"
  def parse(lines: List[String]) = {
    lines.exists(_ contains "The following tournaments are currently in progress:")
  } option {
    lines.collect {
      case Regexp(id, name, status) => parseIntOption(id) map {
        Tourney(_, name.trim, status match {
          case "Round Started" => Relay.Status.Started
          case "Round Over"    => Relay.Status.Finished
          case _               => Relay.Status.Unknown
        })
      }
    }.flatten
  }
  case class Tourney(ficsId: Int, name: String, status: Relay.Status)
  private val Regexp = """^:(\d+)\s+(.+)\s{2,}(.+)$""".r
}

case class ListGames(id: Int) extends Command {
  type Result = ListGames.Result
  val str = s"tell relay listgame $id"
  def parse(lines: List[String]) =
    if (lines.exists(_ contains "There is no tournament with id")) Nil.some
    else lines.exists(_ contains "There are ") option {
      lines.collect {
        case ListGames.Regexp(ficsId, white, black) => parseIntOption(ficsId) map {
          ListGames.Game(_, white, black)
        }
      }.flatten
    }
}
case object ListGames {
  type Result = List[Game]
  case class Game(ficsId: Int, white: String, black: String)
  private val Regexp = """(?i)^:(\d+)\s+([a-z0-9]+)\s+([a-z0-9]+).+$""".r
}

case class GetTime(player: String) extends Command {
  type Result = GetTime.Result
  import GetTime._
  val str = s"time $player"
  def parse(lines: List[String]) =
    lines.mkString("\n") match {
      case Regexp(name, white, black) =>
        if (name == player) toTenths(white) |@| toTenths(black) apply Times.apply match {
          case Some(data) => Success(data).some
          case None       => Failure(new Exception(s"Invalid times $lines")).some
        }
        else Failure(new Exception(s"Got times for the wrong player $player != $name")).some
      case _ => none
    }
}
object GetTime {
  type Result = Try[Times]
  case class Times(white: Int, black: Int)
  private val Regexp =
    ("""(?s)Game \d+: (\w+).*White Clock : ([0-9:\.]+).*Black Clock : ([0-9:\.]+)""").r.unanchored
  // White Clock : 11:01.033
  // White Clock : 1:31:00.000
  def toTenths(clock: String): Option[Int] =
    clock.replace(".", ":").split(":").flatMap(parseIntOption) match {
      case Array(seconds, millis)                 => Some(seconds * 10 + millis / 100)
      case Array(minutes, seconds, millis)        => Some((60 * minutes + seconds) * 10 + millis / 100)
      case Array(hours, minutes, seconds, millis) => Some((60 * 60 * hours + 60 * minutes + seconds) * 10 + millis / 100)
      case _ =>
        println(s"[relay] invalid clock $clock")
        none
    }
}

case class Moves(ficsId: Int) extends Command {
  type Result = Moves.Result
  val str = s"moves $ficsId"
  def parse(lines: List[String]) = Moves.parse(ficsId, lines)
}
case object Moves {
  type Result = Try[Game]
  def parse(ficsId: Int, lines: List[String]) =
    lines.find(_ contains s"Movelist for game $ficsId") map { firstLine =>
      lines collectFirst {
        case PlayersR(wt, wn, wr, bt, bn, br, date) => Game(
          white = Player(wn, wt.some.filter(_.nonEmpty), parseIntOption(wr)),
          black = Player(bn, bt.some.filter(_.nonEmpty), parseIntOption(br)),
          pgn = MoveCommentR.replaceAllIn(
            lines.dropWhile { l =>
              !matches(MoveLineR, l)
            }.takeWhile { l =>
              matches(MoveLineR, l)
            }.mkString(" ").trim,
            ""),
          date = DateTime.now,
          title = firstLine.trim)
      } match {
        case None      => Failure(new Exception(s"Invalid moves data ${lines.headOption}"))
        case Some(res) => Success(res)
      }
    }

  case class Player(name: String, title: Option[String], rating: Option[Int]) {
    def ficsName = s"${~title}$name"
    def splitName = name.split("(?=\\p{Upper})") mkString " "
  }
  case class Game(white: Player, black: Player, pgn: String, date: DateTime, title: String)

  private val MoveLineR = """^\d+\.(\s+[^\s]+){2,4}""".r
  private val MoveCommentR = """\([^\)]+\)""".r
  private val TitleR = """(CM|NM|FM|IM|GM|WGM|WIM|WFM|)"""
  private val NameR = """(\w+)\s\((.+)\)"""
  private val PlayersR = (s"""^${TitleR}${NameR}\\svs\\.\\s${TitleR}${NameR}\\s---\\s(.+)$$""").r

  private def matches(r: Regex, str: String) = r.pattern.matcher(str).matches
}
