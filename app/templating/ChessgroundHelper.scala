package lila.app
package templating

import chess.{ Color, Board }
import play.twirl.api.Html
import lila.api.Context

import lila.game.Pov

trait ChessgroundHelper {

  def chessground(board: Board, orient: Color)(implicit ctx: Context): Html = wrap {
    if (ctx.is3d) ""
    else board.pieces.map {
      case (pos, piece) =>
        val klass = s"${piece.color.name} ${piece.role.name}"
        val top = orient.fold(8 - pos.y, pos.y - 1) * 12.5
        val left = orient.fold(pos.x - 1, 8 - pos.x) * 12.5
        s"""<piece class="$klass" style="top:$top%;left:$left%"></piece>"""
    } mkString ""
  }

  def chessground(pov: Pov)(implicit ctx: Context): Html =
    chessground(pov.game.toChess.board, pov.color)

  private def wrap(content: String) = Html {
    s"""<div class="cg-board-wrap"><div class="cg-board">$content</div></div>"""
  }

  lazy val miniBoardContent = wrap("")
}
