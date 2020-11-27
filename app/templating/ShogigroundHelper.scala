package lila.app
package templating

import chess.{ Board, Color, Pos }
import lila.api.Context

import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

trait ShogigroundHelper {

  private val cgWrap      = div(cls := "cg-wrap")
  private val cgHelper    = tag("cg-helper")
  private val cgContainer = tag("cg-container")
  private val cgBoard     = tag("cg-board")
  val cgWrapContent       = cgHelper(cgContainer(cgBoard))

  def shogiground(board: Board, orient: Color, lastMove: List[Pos] = Nil)(implicit ctx: Context): Frag =
    div(cls := s"cg-wrap orientation-${orient.name}") {
      cgBoard {
        raw {
          if (ctx.pref.is3d) ""
          else {
            def top(p: Pos)  = orient.fold(9 - p.y, p.y - 1) * 11.11
            def left(p: Pos) = orient.fold(p.x - 1, 9 - p.x) * 11.11
            val highlights = ctx.pref.highlight ?? lastMove.distinct.map { pos =>
              s"""<square class="last-move" style="top:${top(pos)}%;left:${left(pos)}%"></square>"""
            } mkString ""
            val pieces =
              if (ctx.pref.isBlindfold) ""
              else
                board.pieces.map {
                  case (pos, piece) =>
                    val klass = s"${piece.color.name} ${piece.role.name}"
                    s"""<piece class="$klass" style="top:${top(pos)}%;left:${left(pos)}%"></piece>"""
                } mkString ""
            s"$highlights$pieces"
          }
        }
      }
    }

  def shogiground(pov: Pov)(implicit ctx: Context): Frag =
    shogiground(
      board = pov.game.board,
      orient = pov.color,
      lastMove = pov.game.history.lastMove.map(_.origDest) ?? {
        case (orig, dest) => List(orig, dest)
      }
    )

  private def wrap(content: Frag): Frag =
    cgWrap {
      cgHelper {
        cgContainer {
          content
        }
      }
    }

  lazy val shogigroundBoard = wrap(cgBoard)
}
