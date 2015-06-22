package lila.relay
package command

import org.joda.time.DateTime
import scala.util.matching.Regex

sealed trait Command {
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
  case class Tourney(id: Int, name: String, status: Relay.Status)
  private val Regexp = """^:(\d+)\s+(.+)\s{2,}(.+)$""".r
}

case class ListGames(id: Int) extends Command {
  type Result = ListGames.Result
  val str = s"tell relay listgame $id"
  def parse(lines: List[String]) =
    if (lines.exists(_ contains "There is no tournament with id")) Nil.some
    else lines.exists(_ contains "There are ") option {
      lines.collect {
        case ListGames.Regexp(id, white, black) => parseIntOption(id) map {
          ListGames.Game(_, white, black)
        }
      }.flatten
    }
}
case object ListGames {
  type Result = List[Game]
  case class Game(id: Int, white: String, black: String)
  private val Regexp = """(?i)^:(\d+)\s+([a-z0-9]+)\s+([a-z0-9]+).+$""".r
}

case class Moves(id: Int) extends Command {
  type Result = Moves.Result
  val str = s"moves $id"
  def parse(lines: List[String]) = Moves parse lines
}
case object Moves {
  type Result = Game
  def parse(lines: List[String]) =
    lines.exists(_ contains "Movelist for game ") ?? {
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
          date = DateTime.now)
      }
    }

  case class Player(name: String, title: Option[String], rating: Option[Int])
  case class Game(white: Player, black: Player, pgn: String, date: DateTime)

  private val MoveLineR = """^\d+\.(\s+[^\s]+){2,4}""".r
  private val MoveCommentR = """\([^\)]+\)""".r
  private val TitleR = """(CM|NM|FM|IM|GM|WGM|)"""
  private val NameR = """(\w+)\s\((.+)\)"""
  private val PlayersR = (s"""^${TitleR}${NameR}\\svs\\.\\s${TitleR}${NameR}\\s---\\s(.+)$$""").r

  private def matches(r: Regex, str: String) = r.pattern.matcher(str).matches
}
