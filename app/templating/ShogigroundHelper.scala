package lila.app
package templating

import shogi.{ Board, Color, Pos }
import lila.api.Context

import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

trait ShogigroundHelper {

  private val cgWrap      = div(cls := "cg-wrap")
  private val cgContainer = tag("cg-container")
  private val cgBoard     = tag("cg-board")
  val cgWrapContent       = cgContainer(cgBoard)

  def shogiground(board: Board, orient: Color, lastMove: List[Pos] = Nil)(implicit ctx: Context): Frag =
    div(cls := s"cg-wrap orientation-${orient.name}") {
      cgBoard {
        raw {
          def top(p: Pos)  = orient.fold(p.y - 1, board.variant.numberOfRanks - p.y) * (100 / board.variant.numberOfRanks)
          def left(p: Pos) = orient.fold(board.variant.numberOfFiles - p.x, p.x - 1) * (100 / board.variant.numberOfFiles)
          val highlights = ctx.pref.highlight ?? lastMove.distinct.map { pos =>
            s"""<square class="last-move" style="top:${top(pos)}%;left:${left(pos)}%"></square>"""
          } mkString ""
          val pieces =
            if (ctx.pref.isBlindfold) ""
            else
              board.pieces.map { case (pos, piece) =>
                val klass = s"${piece.color.name} ${piece.role.name}"
                s"""<piece class="$klass" style="top:${top(pos)}%;left:${left(pos)}%"></piece>"""
              } mkString ""
          s"$highlights$pieces"
        }
      }
    }

  def shogiground(pov: Pov)(implicit ctx: Context): Frag =
    shogiground(
      board = pov.game.board,
      orient = pov.color,
      lastMove = pov.game.history.lastMove.map(_.origDest) ?? { case (orig, dest) =>
        List(orig, dest)
      }
    )

  private def wrap(content: Frag): Frag =
    cgWrap {
      cgContainer {
        content
      }
    }

  lazy val shogigroundBoard = wrap(cgBoard)
}
