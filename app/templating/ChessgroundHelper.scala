package lila.app
package templating

import chess.{ Board, Color, Square }
import lila.api.WebContext

import lila.app.ui.ScalatagsTemplate.*
import lila.game.Pov

trait ChessgroundHelper:

  private val cgWrap      = div(cls := "cg-wrap")
  private val cgContainer = tag("cg-container")
  private val cgBoard     = tag("cg-board")
  val cgWrapContent       = cgContainer(cgBoard)

  def chessground(board: Board, orient: Color, lastMove: List[Square] = Nil)(using ctx: WebContext): Frag =
    wrap {
      cgBoard {
        raw {
          if (ctx.pref.is3d) ""
          else
            def top(p: Square)  = orient.fold(7 - p.rank.index, p.rank.index) * 12.5
            def left(p: Square) = orient.fold(p.file.index, 7 - p.file.index) * 12.5
            val highlights = ctx.pref.highlight so lastMove.distinct.map { pos =>
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

  def chessground(pov: Pov)(using ctx: WebContext): Frag =
    chessground(
      board = pov.game.board,
      orient = pov.color,
      lastMove = pov.game.history.lastMove
        .map(_.origDest)
        .so: (orig, dest) =>
          List(orig, dest)
    )

  private def wrap(content: Frag): Frag =
    cgWrap {
      cgContainer {
        content
      }
    }

  lazy val chessgroundBoard = wrap(cgBoard)
