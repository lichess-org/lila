package lila.ui

import chess.{ Board, Color, Square }
import chess.format.{ Fen, Uci }

import lila.ui.*
import ScalatagsTemplate.*
import lila.core.pref.Pref

object ChessHelper:

  def underscoreFen(fen: chess.format.Fen.Full) = fen.value.replace(" ", "_")

trait ChessHelper:

  export ChessHelper.*

  private val cgWrap      = div(cls := "cg-wrap")
  private val cgContainer = tag("cg-container")
  private val cgBoard     = tag("cg-board")
  private val dataState   = attr("data-state")
  val cgWrapContent       = cgContainer(cgBoard)

  def chessgroundMini(fen: Fen.Board, color: Color = chess.White, lastMove: Option[Uci] = None)(
      tag: Tag
  ): Tag =
    tag(
      cls       := "mini-board mini-board--init cg-wrap is2d",
      dataState := s"${fen.value},${color.name},${lastMove.so(_.uci)}"
    )(cgWrapContent)

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
