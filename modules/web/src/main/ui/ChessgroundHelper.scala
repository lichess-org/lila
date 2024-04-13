package lila.web
package ui

import chess.{ Board, Color, Square }

import lila.web.ui.ScalatagsTemplate.*
import lila.core.pref.Pref

trait ChessgroundHelper:

  private val cgWrap      = div(cls := "cg-wrap")
  private val cgContainer = tag("cg-container")
  private val cgBoard     = tag("cg-board")
  val cgWrapContent       = cgContainer(cgBoard)

  def chessground(
      board: Board,
      orient: Color,
      lastMove: List[Square] = Nil,
      blindfold: Boolean,
      pref: Pref
  ): Frag =
    wrap {
      cgBoard {
        raw {
          if pref.is3d then ""
          else
            def top(p: Square)  = orient.fold(7 - p.rank.value, p.rank.value) * 12.5
            def left(p: Square) = orient.fold(p.file.value, 7 - p.file.value) * 12.5
            val highlights = pref.highlight
              .so(lastMove.distinct.map { pos =>
                s"""<square class="last-move" style="top:${top(pos)}%;left:${left(pos)}%"></square>"""
              })
              .mkString("")
            val pieces =
              if blindfold then ""
              else
                board.pieces
                  .map { (pos, piece) =>
                    val klass = s"${piece.color.name} ${piece.role.name}"
                    s"""<piece class="$klass" style="top:${top(pos)}%;left:${left(pos)}%"></piece>"""
                  }
                  .mkString("")
            s"$highlights$pieces"
        }
      }
    }

  private def wrap(content: Frag): Frag =
    cgWrap {
      cgContainer {
        content
      }
    }

  lazy val chessgroundBoard = wrap(cgBoard)
