package lila.ui

import chess.format.{ Fen, Uci }
import chess.Color

import lila.ui.*

import ScalatagsTemplate.*

object ChessHelper:

  def underscoreFen(fen: chess.format.Fen.Full) = fen.value.replace(" ", "_")

trait ChessHelper:

  export ChessHelper.*

  private val cgWrap = div(cls := "cg-wrap")
  private val cgContainer = tag("cg-container")
  private val dataState = attr("data-state")
  val cgBoard = tag("cg-board")
  val cgWrapContent = cgContainer(cgBoard)

  def chessgroundMini(fen: Fen.Board, color: Color = chess.White, lastMove: Option[Uci] = None)(
      tag: Tag
  ): Tag =
    tag(
      cls := "mini-board mini-board--init cg-wrap is2d",
      dataState := s"${fen.value},${color.name},${lastMove.so(_.uci)}"
    )(cgWrapContent)

  def chessgroundWrap(content: Frag): Frag =
    cgWrap:
      cgContainer:
        content

  lazy val chessgroundBoard = chessgroundWrap(cgBoard)
