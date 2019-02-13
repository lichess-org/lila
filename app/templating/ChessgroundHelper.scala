package lila.app
package templating

import chess.{ Color, Board, Pos }
import lila.api.Context
import play.twirl.api.Html

import lila.game.Pov

trait ChessgroundHelper {

  def chessground(board: Board, orient: Color, lastMove: List[Pos] = Nil)(implicit ctx: Context): Html = wrap {
    if (ctx.pref.is3d) ""
    else {
      def top(p: Pos) = orient.fold(8 - p.y, p.y - 1) * 12.5
      def left(p: Pos) = orient.fold(p.x - 1, 8 - p.x) * 12.5
      val highlights = ctx.pref.highlight ?? lastMove.distinct.map { pos =>
        s"""<square class="last-move" style="top:${top(pos)}%;left:${left(pos)}%"></square>"""
      } mkString ""
      val pieces =
        if (ctx.pref.isBlindfold) ""
        else board.pieces.map {
          case (pos, piece) =>
            val klass = s"${piece.color.name} ${piece.role.name}"
            s"""<piece class="$klass" style="top:${top(pos)}%;left:${left(pos)}%"></piece>"""
        } mkString ""
      s"$highlights$pieces"
    }
  }

  def chessground(pov: Pov)(implicit ctx: Context): Html = chessground(
    board = pov.game.board,
    orient = pov.color,
    lastMove = pov.game.history.lastMove.map(_.origDest) ?? {
      case (orig, dest) => List(orig, dest)
    }
  )

  private def wrap(content: String) = Html {
    s"""<div class="cg-board-wrap"><div class="cg-board">$content</div></div>"""
  }

  lazy val miniBoardContent = wrap("")

  lazy val chessgroundSvg = wrap("<svg></svg>")
}
