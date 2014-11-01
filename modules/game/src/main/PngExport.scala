package lila.game

import chess.format.Forsyth
import java.io.{ File, OutputStream }
import scala.sys.process._

object PngExport {

  private val logger = ProcessLogger(_ => (), println)

  def apply(execPath: String)(game: Game)(out: OutputStream) {
    val fen = (Forsyth >> game.toChess).split(' ').head
    val color = game.firstColor.letter.toString
    val lastMove = ~game.castleLastMoveTime.lastMoveString
    val exec = Process(Seq("php", "board-creator.php", fen, color, lastMove), new File(execPath))
    exec #> out ! logger
  }
}
