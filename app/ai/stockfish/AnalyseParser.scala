package lila
package ai.stockfish

import analyse.Info

object AnalyseParser {

  def apply(lines: List[String]): Info =
    Info(
      cp = none,
      mate = none,
      best = findBestMove(lines)
    )

  private val bestMoveRegex = """^bestmove\s(\w+)(?=\s.+)?$""".r

  private def findBestMove(lines: List[String]) =
    lines.lastOption map { line ⇒
      bestMoveRegex.replaceAllIn(line, m ⇒ m group 1)
    } flatMap Uci.parseMove
}
