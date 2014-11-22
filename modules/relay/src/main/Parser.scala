package lila.relay

import scala.util.matching.Regex

private[relay] object Parser {

  private val MoveLineR = """^\d+\.(\s+[^\s]+){2,4}""".r
  private val MoveCommentR = """\([^\)]+\)""".r
  private val PlayersR = """^(\w+\s\(.+\))\svs\.\s(\w+\s\(.+\))\s---\s.+$""".r

  private def matches(r: Regex, str: String) = r.pattern.matcher(str).matches

  case class Data(
    pgn: String,
    white: String,
    black: String)

  def game(str: String): Option[Data] = {
    val lines = str.split(Array('\r', '\n')).toList.filter(_.nonEmpty).map(_.trim)
    val pgn = MoveCommentR.replaceAllIn(
      lines.dropWhile { l =>
        !matches(MoveLineR, l)
      }.takeWhile { l =>
        matches(MoveLineR, l)
      }.mkString(" ").trim,
      "")
    lines collectFirst {
      case PlayersR(white, black) => Data(pgn, white, black)
    }
  }

  def move(str: String) = str.split(' ') lift 29 err s"Can't parse move out of $str"
}
