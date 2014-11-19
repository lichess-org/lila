package lila.relay

import scala.util.matching.Regex

import lila.game.{ Game, Player, Source }
import lila.importer.ImportData

private[relay] object Parser {

  private val MoveLineR = """^\s*\d+\.(\s+[^\s]+){4}""".r
  private val MoveCommentR = """\([^\)]+\)""".r

  private def matches(r: Regex, str: String) = r.pattern.matcher(str).matches

  def pgn(str: String) = ImportData {
    MoveCommentR.replaceAllIn(
      str.lines.toList.dropWhile { l =>
        !matches(MoveLineR, l)
      }.takeWhile { l =>
        matches(MoveLineR, l)
      }.mkString(" ").trim,
      "")
  }
}
