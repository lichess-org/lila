package lila
package model

import Pos._

class RookTest extends LilaSpec {

  //"a rook" should {

    //val rook = White - Rook

    //def boardWithRookAt(pos: Pos) = Board.empty place rook at pos

    //val board = boardWithRookAt(F6)

    //"be able to move to any position along the same rank or file" in {
      //rook.movesFrom(position('e4)).keySet must containPositionLists(
        //List('e5, 'e6, 'e7, 'e8), List('e3, 'e2, 'e1), List('f4, 'g4, 'h4), List('d4, 'c4, 'b4, 'a4))
    //}

    //"be able to move to any position along the same rank or file, even when at the edges" in {
      //rook.movesFrom(position('h8)).keySet must containPositionLists(
        //List('h7, 'h6, 'h5, 'h4, 'h3, 'h2, 'h1), List('g8, 'f8, 'e8, 'd8, 'c8, 'b8, 'a8))
    //}

    //"require that positions can only be moved to if they aren't occupied by the same colour" in {
      //rook.movesFrom(position('d4)).elements.foreach {
        //element ⇒
          //val query = element._2._1
          //element._1.foreach {
            //position ⇒
              //query(board, position, Nil) must_== Continue
              //query(new Board(board.pieces(position) = Piece(Black, Pawn), Nil), position, Nil) must_== IncludeAndStop
              //query(new Board(board.pieces(position) = Piece(White, Pawn), Nil), position, Nil) must_== Stop
          //}
      //}
    //}

    //"invoke the correct board movements if the option is taken" in {
      //rook.movesFrom(position('f6)).elements.foreach {
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
  //}
}
