package lila
package model

import Pos._
import format.Visual

class RookTest extends LilaSpec {

  "a rook" should {

    val rook = White - Rook
    val board = Board.empty

    def boardWithRookAt(pos: Pos): Valid[Board] = board place rook at pos
    def basicMoves(pos: Pos): Valid[Set[Pos]] = boardWithRookAt(pos) map { b ⇒
      rook.basicMoves(pos, b)
    }

    "be able to move to any position along the same rank or file" in {
      basicMoves(E4) must bePoss(E5, E6, E7, E8, E3, E2, E1, F4, G4, H4, D4, C4, B4, A4)
    }

    "be able to move to any position along the same rank or file, even when at the edges" in {
      basicMoves(H8) must bePoss(H7, H6, H5, H4, H3, H2, H1, G8, F8, E8, D8, C8, B8, A8)
    }

    "require that positions can only be moved to if they aren't occupied by the same colour" in {
      val board = Visual << """
k B



N R    P

PPPPPPPP
 NBQKBNR
"""
      board pieceAt C4 map { _.basicMoves(C4, board) } must bePoss(
        C3, C5, C6, C7, B4, D4, E4, F4, G4)
    }

    //"invoke the correct board movements if the option is taken" in {
    //rook.movesFrom(F6).elements.foreach {
    //element ⇒
    //val implication = element._2._2
    //element._1.foreach {
    //toPosition ⇒
    //val boardAfterMove = boardWithRookAt(toPosition)
    //val boardWithBlackPieceAtTarget = new Board(board.pieces(toPosition) = Piece(Black, Pawn), Nil)
    //val boardWithWhitePieceAtTarget = new Board(board.pieces(toPosition) = Piece(White, Pawn), Nil)
    //val boardAfterPieceIsTaken = new Board(boardWithRookAt(toPosition).pieces, Piece(Black, Pawn) :: Nil)
    //implication(board, toPosition, Nil) must_== boardAfterMove
    //implication(boardWithWhitePieceAtTarget, toPosition, Nil) must throwAn[IllegalMoveException]
    //implication(boardWithBlackPieceAtTarget, toPosition, Nil) must_== boardAfterPieceIsTaken
    //}
    //}
    //}
  }
}
