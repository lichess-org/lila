package lila.relay

import lila.game.{ Game, Player, Source }
import scala.util.matching.Regex

private[relay] object Parser {

  private val MoveLineR = """^\s*\d+\.(\s+[^\s]+){4}""".r
  private val MoveCommentR = """\([^\)]+\)""".r

  private def matches(r: Regex, str: String) = r.pattern.matcher(str).matches

  def game(str: String): Option[Game] = {
    val pgn = MoveCommentR.replaceAllIn(
      str.lines.toList.dropWhile { l =>
        !matches(MoveLineR, l)
      }.takeWhile { l =>
        matches(MoveLineR, l)
      }.mkString(" ").trim,
      "")
    println(pgn)
    val variant = chess.Variant.Standard
    val g = Game.make(
      game = chess.Game(variant),
      whitePlayer = Player.white,
      blackPlayer = Player.black,
      mode = chess.Mode.Casual,
      variant = variant,
      source = Source.ImportLive,
      pgnImport = none).start
    g.some
  }
}
