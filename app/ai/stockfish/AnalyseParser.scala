package lila
package ai.stockfish

import analyse.Info

object AnalyseParser {

  def apply(lines: List[String]): String ⇒ Valid[Info] =
    moveString ⇒ for {
      move ← Uci parseMove moveString toValid "Invalid move " + moveString
      bestString ← findBestMove(lines) toValid "Analysis bestmove not found"
      best ← Uci parseMove bestString toValid "Invalid bestmove " + bestString
    } yield Info(
      move = move,
      best = best,
      cp = findCp(lines),
      mate = findMate(lines))

  private val bestMoveRegex = """^bestmove\s(\w+).*$""".r
  private def findBestMove(lines: List[String]) =
    lines.headOption map { line ⇒
      bestMoveRegex.replaceAllIn(line, m ⇒ m group 1)
    }

  private val cpRegex = """^info.*\scp\s(\-?\d+).*$""".r
  private def findCp(lines: List[String]) =
    lines.tail.headOption map { line ⇒
      cpRegex.replaceAllIn(line, m ⇒ m group 1)
    } flatMap parseIntOption

  private val mateRegex = """^info.*\smate\s(\-?\d+).*$""".r
  private def findMate(lines: List[String]) =
    lines.tail.headOption map { line ⇒
      mateRegex.replaceAllIn(line, m ⇒ m group 1)
    } flatMap parseIntOption
}
