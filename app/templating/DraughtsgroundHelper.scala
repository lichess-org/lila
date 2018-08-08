package lidraughts.app
package templating

import draughts.{ Color, Board, Pos }
import lidraughts.api.Context
import play.twirl.api.Html

import lidraughts.game.Pov

trait DraughtsgroundHelper {

  def draughtsground(board: Board, orient: Color, lastMove: List[Pos] = Nil)(implicit ctx: Context): Html = wrap {
    def addX(p: Pos) = if (p.y % 2 != 0) -0.5 else -1.0
    def top(p: Pos) = orient.fold(p.y - 1, 10 - p.y) * 10.0
    def left(p: Pos) = orient.fold(addX(p) + p.x, 4.5 - (addX(p) + p.x)) * 20.0
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

  def draughtsground(pov: Pov)(implicit ctx: Context): Html = draughtsground(
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
}
