package lila
package ai.stockfish

import analyse.Info

object AnalyseParser {

  def apply(lines: List[String]): String ⇒ Valid[Info] =
    move ⇒
      findBestMove(lines) toValid "Analysis bestmove not found" flatMap { best ⇒
        Info(move, best, findCp(lines), findMate(lines))
      }

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
