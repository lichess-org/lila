package lila.app
package ui

import chess.{ Pos, Color => ChessColor }
import Pos._

import lila.game.Pov

object Board {

  def render(pov: Pov) = {
    val check = pov.game.check.??(_.key)
    val board = pov.game.toChess.board
    val moved: Pos => Boolean =
      pov.game.toChessHistory.lastMove.fold((_: Pos) => false) { last =>
        pos => last._1 == pos || last._2 == pos
      }
    pov.color.fold(white, black) map { s =>
      val ccheck = if (s.pos.key == check) " check" else ""
      val cmoved = if (moved(s.pos)) " moved" else ""
      s"""<div class="lcs ${s.color}$ccheck$cmoved" id="${s.pos.key}" style="top:${s.top}px;left:${s.left}px">""" ++
        """<div class="lcsi"></div>""" ++ {
          board(s.pos).??(piece =>
            s"""<div class="piece ${piece.role.name} ${piece.color.name}"></div>"""
          )
        } ++
        "</div>"
    } mkString
  }

  private final class Square(
    val pos: Pos,
    val color: ChessColor,
    val top: Int,
    val left: Int)

  private def whitePoses = List(A1, A2, A3, A4, A5, A6, A7, A8, B1, B2, B3, B4, B5, B6, B7, B8, C1, C2, C3, C4, C5, C6, C7, C8, D1, D2, D3, D4, D5, D6, D7, D8, E1, E2, E3, E4, E5, E6, E7, E8, F1, F2, F3, F4, F5, F6, F7, F8, G1, G2, G3, G4, G5, G6, G7, G8, H1, H2, H3, H4, H5, H6, H7, H8)

  private def blackPoses = whitePoses.reverse

  private val white = make(whitePoses)

  private val black = make(blackPoses)

  private def make(poses: List[Pos]) = {
    for {
      rank ← (poses grouped 8).zipWithIndex
      (files, y) = rank
      file ← files.zipWithIndex
      (pos, x) = file
      color = ChessColor((x + y) % 2 == 1)
    } yield new Square(pos, color, 64 * (8 - (x + 1)), 64 * y)
  } toList
}
