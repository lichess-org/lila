package lila.game

import chess.format.Forsyth
import java.io.{ File, OutputStream }
import scala.sys.process._

object PngExport {

  private val logger = ProcessLogger(
    x => play.api.Logger("png").info(x),
    x => play.api.Logger("png").error(x))

  def apply(execPath: String)(game: Game)(out: OutputStream) {
    val fen = (Forsyth >> game.toChess).split(' ').head
    val color = game.firstColor.letter.toString
    val lastMove = ~game.castleLastMoveTime.lastMoveString
    val parts = Seq("php", "board-creator.php", fen, color, lastMove)
    val exec = Process(parts, new File(execPath))
    play.api.Logger("png").warn(parts mkString " ")
    exec #> out ! logger
  }
}
