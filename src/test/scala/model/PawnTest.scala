package lila
package model

import Pos._
import format.Visual

class PawnTest extends LilaSpec {

  "a white pawn" should {

    val color = White
    val pawn = color - Pawn

    def basicMoves(pos: Pos): Valid[Set[Pos]] = Board.empty place pawn at pos map { b ⇒
      pawn.basicMoves(pos, b)
    }

    "move towards rank by 1 square" in {
      basicMoves(A4) must bePoss(A5)
    }

    "not move to positions that are occupied by the same colour" in {
      (for {
        board ← Board.empty.seq(
          _ place White - Pawn at A4,
          _ place White - Pawn at A5)
        piece ← board pieceAt A4
      } yield piece.basicMoves(A4, board)) must bePoss()
    }
  }

  "a black pawn" should {

    val color = Black
    val pawn = color - Pawn

    def basicMoves(pos: Pos): Valid[Set[Pos]] = Board.empty place pawn at pos map { b ⇒
      pawn.basicMoves(pos, b)
    }

    "move towards rank by 1 square" in {
      basicMoves(A4) must bePoss(A3)
    }

    "not move to positions that are occupied by the same colour" in {
      (for {
        board ← Board.empty.seq(
          _ place color - Pawn at A4,
          _ place color - Pawn at A3)
        piece ← board pieceAt A4
      } yield piece.basicMoves(A4, board)) must bePoss()
    }
  }
}
