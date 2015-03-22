package lila.puzzle

import chess.format.Forsyth
import java.io.{ File, OutputStream }
import scala.sys.process._

object PngExport {

  private val logger = ProcessLogger(_ => (), _ => ())

  def apply(execPath: String)(puzzle: Puzzle)(out: OutputStream) {
    val color = puzzle.color.letter.toString
    val lastMove = puzzle.initialMove
    val fen = (puzzle.fenAfterInitialMove | puzzle.fen).takeWhile(' ' !=)
    val exec = Process(Seq("php", "board-creator.php", fen, color, lastMove), new File(execPath))
    exec #> out ! logger
  }
}
