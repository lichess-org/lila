package lila
package model

import Pos._

object BoardTest extends LilaSpec {

  val board = Board()

  "a board" should {

    "have pieces by default" in {
      board.pieces must not beEmpty
    }

    "allow a piece to be placed" in {
      board place White - Rook at 'e3 must beSuccess.like {
        case b ⇒ b('e3) mustEqual Some(White - Rook)
      }
    }

    "allow a piece to be taken" in {
      board take 'a1 must beSuccess.like {
        case b ⇒ b('a1) must beNone
      }
    }

    //"allow a pawn to be promoted to a queen" in {
    //var updatedBoard = board place Piece(Black, Pawn) at 'a8
    //updatedBoard = updatedBoard promote position('a8) to Queen
    //updatedBoard.pieces must_== Map(position('a8) -> Piece(Black, Queen))
    //}

    //"allow a pawn to be promoted to a bishop" in {
    //var updatedBoard = board place Piece(Black, Pawn) at 'f8
    //updatedBoard = updatedBoard promote position('f8) to Bishop
    //updatedBoard.pieces must_== Map(position('f8) -> Piece(Black, Bishop))
    //}

    //"allow a pawn to be promoted to a knight" in {
    //var updatedBoard = board place Piece(White, Pawn) at 'g1
    //updatedBoard = updatedBoard promote position('g1) to Knight
    //updatedBoard.pieces must_== Map(position('g1) -> Piece(White, Knight))
    //}

    //"allow a pawn to be promoted to a rook" in {
    //var updatedBoard = board place Piece(White, Pawn) at 'd1
    //updatedBoard = updatedBoard promote position('d1) to Rook
    //updatedBoard.pieces must_== Map(position('d1) -> Piece(White, Rook))
    //}

    //"no-op when promoting a pawn to a pawn" in {
    //var updatedBoard = board place Piece(White, Pawn) at 'd2
    //updatedBoard = updatedBoard promote position('d2) to Pawn
    //updatedBoard.pieces must_== Map(position('d2) -> Piece(White, Pawn))
    //}

    //"not allow a pawn to be promoted to a king" in {
    //val updatedBoard = board place Piece(White, Pawn) at 'd2
    //(updatedBoard promote position('d2) to King) must throwAn[IllegalPromotionException]
    //}

    //"not allow an empty position to be promoted" in {
    //(board promote position('a6) to Queen) must throwAn[IllegalPromotionException]
    //}

    //"not allow a non-pawn to be promoted" in {
    //val updatedBoard = board place Piece(Black, Knight) at 'f7
    //(updatedBoard promote position('f7) to Queen) must throwAn[IllegalPromotionException]
    //}

    //"allow a piece to move" in {
    //var updatedBoard = board place Piece(Black, Rook) at 'h5
    //updatedBoard = updatedBoard move position('h5) to position('h8)
    //updatedBoard.pieces must_== Map(position('h8) -> Piece(Black, Rook))
    //}

    //"not allow an empty position to move" in {
    //(board move position('a5) to position('a6)) must throwAn[IllegalMoveException]
    //}

    //"not allow a piece to move to an occupied position" in {
    //var updatedBoard = board place Piece(White, Pawn) at position('e1)
    //updatedBoard = updatedBoard place Piece(Black, Bishop) at position('g3)
    //(updatedBoard move position('g3) to position('e1)) must throwAn[IllegalMoveException]
    //}

    //"be able to be reset to the starting game layout" in {
    //val newBoard = board.reset
    //newBoard.pieces must_== Map(
    //position('a1) -> Piece(White, Rook),
    //position('b1) -> Piece(White, Knight),
    //position('c1) -> Piece(White, Bishop),
    //position('d1) -> Piece(White, Queen),
    //position('e1) -> Piece(White, King),
    //position('f1) -> Piece(White, Bishop),
    //position('g1) -> Piece(White, Knight),
    //position('h1) -> Piece(White, Rook),
    //position('a2) -> Piece(White, Pawn),
    //position('b2) -> Piece(White, Pawn),
    //position('c2) -> Piece(White, Pawn),
    //position('d2) -> Piece(White, Pawn),
    //position('e2) -> Piece(White, Pawn),
    //position('f2) -> Piece(White, Pawn),
    //position('g2) -> Piece(White, Pawn),
    //position('h2) -> Piece(White, Pawn),
    //position('a7) -> Piece(Black, Pawn),
    //position('b7) -> Piece(Black, Pawn),
    //position('c7) -> Piece(Black, Pawn),
    //position('d7) -> Piece(Black, Pawn),
    //position('e7) -> Piece(Black, Pawn),
    //position('f7) -> Piece(Black, Pawn),
    //position('g7) -> Piece(Black, Pawn),
    //position('h7) -> Piece(Black, Pawn),
    //position('a8) -> Piece(Black, Rook),
    //position('b8) -> Piece(Black, Knight),
    //position('c8) -> Piece(Black, Bishop),
    //position('d8) -> Piece(Black, Queen),
    //position('e8) -> Piece(Black, King),
    //position('f8) -> Piece(Black, Bishop),
    //position('g8) -> Piece(Black, Knight),
    //position('h8) -> Piece(Black, Rook)
    //)
    //}

    //"be able to be rewound to a prior state" in {
    //val newBoard = board.reset
    //val afterMove = newBoard move position('a2) to position('a4)
    //val history = History(Move(Piece(White, Pawn), position('a2), position('a4)), None)
    //afterMove rewind history must_== newBoard
    //}

    //"be able to be rewound to a prior state, including replacement of captured pieces" in {
    //val newBoard = board.reset.move(position('e2)).to(position('e4)).move(position('d7)).to(position('d5)).take(position('h8))
    //val afterMove = newBoard.take(position('d5)).move(position('e4)).to(position('d5))
    //val history = History(Move(Piece(White, Pawn), position('e4), position('d5)), Some(Take(position('d5))))
    //afterMove rewind history must_== newBoard
    //}

    //"be able to be unwound to a future state" in {
    //val newBoard = board.reset
    //val afterMove = newBoard move position('a2) to position('a4)
    //val history = History(Move(Piece(White, Pawn), position('a2), position('a4)), None)
    //newBoard unwind history must_== afterMove
    //}

    //"be able to be unwound to a future state, including capturing of pieces" in {
    //val newBoard = board.reset.move(position('e2)).to(position('e4)).move(position('d7)).to(position('d5)).take(position('h8))
    //val afterMove = newBoard.take(position('d5)).move(position('e4)).to(position('d5))
    //val history = History(Move(Piece(White, Pawn), position('e4), position('d5)), Some(Take(position('d5))))
    //newBoard unwind history must_== afterMove
    //}

    //"advise which positions currently threaten a given position, given a side (opposing pawns)" in {
    //val newBoard = board.place(Piece(White, Pawn)).at(position('f6))
    //(newBoard threatsTo Black at 'g7) must containPositions('f6)
    //}

    //"advise which positions currently threaten a given position, given a side (opposing non-threatening piece)" in {
    //val newBoard = board.place(Piece(White, Knight)).at(position('f6))
    //(newBoard threatsTo Black at 'g7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (multiple pawn opponents)" in {
    //val newBoard = board.place(Piece(White, Pawn)).at(position('f6)).place(Piece(White, Pawn)).at(position('h6))
    //(newBoard threatsTo Black at 'g7) must containPositions('f6, 'h6)
    //}

    //"advise which positions currently threaten a given position, given a side (same side pawns)" in {
    //val newBoard = board.place(Piece(Black, Pawn)).at(position('f6))
    //(newBoard threatsTo Black at 'g7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing rooks)" in {
    //val newBoard = board.place(Piece(White, Rook)).at(position('b5))
    //(newBoard threatsTo Black at 'a5) must containPositions('b5)
    //(newBoard threatsTo Black at 'h5) must containPositions('b5)
    //(newBoard threatsTo Black at 'b8) must containPositions('b5)
    //(newBoard threatsTo Black at 'b1) must containPositions('b5)
    //}

    //"advise which positions currently threaten a given position, given a side (same side rooks)" in {
    //val newBoard = board.place(Piece(Black, Rook)).at(position('b5))
    //(newBoard threatsTo Black at 'a5) must beEmpty
    //(newBoard threatsTo Black at 'h5) must beEmpty
    //(newBoard threatsTo Black at 'b8) must beEmpty
    //(newBoard threatsTo Black at 'b1) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed rook)" in {
    //val newBoard = board.place(Piece(White, Rook)).at(position('b5)).place(Piece(Black, Rook)).at(position('d5))
    //(newBoard threatsTo Black at 'e5) must beEmpty
    //(newBoard threatsTo Black at 'd5) must containPositions('b5)
    //(newBoard threatsTo Black at 'c5) must containPositions('b5)
    //(newBoard threatsTo Black at 'b5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing bishops)" in {
    //val newBoard = board.place(Piece(Black, Bishop)).at(position('f7))
    //(newBoard threatsTo White at 'g8) must containPositions('f7)
    //(newBoard threatsTo White at 'g6) must containPositions('f7)
    //(newBoard threatsTo White at 'e8) must containPositions('f7)
    //(newBoard threatsTo White at 'a2) must containPositions('f7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side bishops)" in {
    //val newBoard = board.place(Piece(Black, Bishop)).at(position('d6))
    //(newBoard threatsTo Black at 'f4) must beEmpty
    //(newBoard threatsTo Black at 'f8) must beEmpty
    //(newBoard threatsTo Black at 'b8) must beEmpty
    //(newBoard threatsTo Black at 'b4) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed bishops)" in {
    //val newBoard = board.place(Piece(White, Rook)).at(position('b5)).place(Piece(Black, Bishop)).at(position('d7))
    //(newBoard threatsTo White at 'a4) must beEmpty
    //(newBoard threatsTo White at 'b5) must containPositions('d7)
    //(newBoard threatsTo White at 'c6) must containPositions('d7)
    //(newBoard threatsTo White at 'd7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing queens on rank/file)" in {
    //val newBoard = board.place(Piece(White, Queen)).at(position('b5))
    //(newBoard threatsTo Black at 'a5) must containPositions('b5)
    //(newBoard threatsTo Black at 'h5) must containPositions('b5)
    //(newBoard threatsTo Black at 'b8) must containPositions('b5)
    //(newBoard threatsTo Black at 'b1) must containPositions('b5)
    //}

    //"advise which positions currently threaten a given position, given a side (same side queen on rank/file)" in {
    //val newBoard = board.place(Piece(Black, Queen)).at(position('b5))
    //(newBoard threatsTo Black at 'a5) must beEmpty
    //(newBoard threatsTo Black at 'h5) must beEmpty
    //(newBoard threatsTo Black at 'b8) must beEmpty
    //(newBoard threatsTo Black at 'b1) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed queen on rank/file)" in {
    //val newBoard = board.place(Piece(White, Queen)).at(position('b5)).place(Piece(Black, Rook)).at(position('d5))
    //(newBoard threatsTo Black at 'e5) must beEmpty
    //(newBoard threatsTo Black at 'd5) must containPositions('b5)
    //(newBoard threatsTo Black at 'c5) must containPositions('b5)
    //(newBoard threatsTo Black at 'b5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing queens on diagonal)" in {
    //val newBoard = board.place(Piece(Black, Queen)).at(position('f7))
    //(newBoard threatsTo White at 'g8) must containPositions('f7)
    //(newBoard threatsTo White at 'g6) must containPositions('f7)
    //(newBoard threatsTo White at 'e8) must containPositions('f7)
    //(newBoard threatsTo White at 'a2) must containPositions('f7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side queen on diagonal)" in {
    //val newBoard = board.place(Piece(Black, Queen)).at(position('d6))
    //(newBoard threatsTo Black at 'f4) must beEmpty
    //(newBoard threatsTo Black at 'f8) must beEmpty
    //(newBoard threatsTo Black at 'b8) must beEmpty
    //(newBoard threatsTo Black at 'b4) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed queen on diagonal)" in {
    //val newBoard = board.place(Piece(White, Rook)).at(position('b5)).place(Piece(Black, Queen)).at(position('d7))
    //(newBoard threatsTo White at 'a4) must beEmpty
    //(newBoard threatsTo White at 'b5) must containPositions('d7)
    //(newBoard threatsTo White at 'c6) must containPositions('d7)
    //(newBoard threatsTo White at 'd7) must beEmpty
    //}
    //}

    //"advise which positions currently threaten a given position, given a side (opposing knights)" in {
    //val newBoard = board.place(Piece(Black, Knight)).at(position('f7))
    //(newBoard threatsTo White at 'd8) must containPositions('f7)
    //(newBoard threatsTo White at 'h8) must containPositions('f7)
    //(newBoard threatsTo White at 'd6) must containPositions('f7)
    //(newBoard threatsTo White at 'h6) must containPositions('f7)
    //(newBoard threatsTo White at 'e5) must containPositions('f7)
    //(newBoard threatsTo White at 'g5) must containPositions('f7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side knights)" in {
    //val newBoard = board.place(Piece(Black, Knight)).at(position('f7))
    //(newBoard threatsTo Black at 'd8) must beEmpty
    //(newBoard threatsTo Black at 'h8) must beEmpty
    //(newBoard threatsTo Black at 'd6) must beEmpty
    //(newBoard threatsTo Black at 'h6) must beEmpty
    //(newBoard threatsTo Black at 'e5) must beEmpty
    //(newBoard threatsTo Black at 'g5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing kings)" in {
    //val newBoard = board.place(Piece(Black, King)).at(position('b4))
    //(newBoard threatsTo White at 'b3) must containPositions('b4)
    //(newBoard threatsTo White at 'b5) must containPositions('b4)
    //(newBoard threatsTo White at 'a3) must containPositions('b4)
    //(newBoard threatsTo White at 'a4) must containPositions('b4)
    //(newBoard threatsTo White at 'a5) must containPositions('b4)
    //(newBoard threatsTo White at 'c3) must containPositions('b4)
    //(newBoard threatsTo White at 'c4) must containPositions('b4)
    //(newBoard threatsTo White at 'c5) must containPositions('b4)
    //}

    //"advise which positions currently threaten a given position, given a side (same side kings)" in {
    //val newBoard = board.place(Piece(Black, King)).at(position('a3))
    //(newBoard threatsTo Black at 'a4) must beEmpty
    //(newBoard threatsTo Black at 'a2) must beEmpty
    //(newBoard threatsTo Black at 'b2) must beEmpty
    //(newBoard threatsTo Black at 'b3) must beEmpty
    //(newBoard threatsTo Black at 'b4) must beEmpty
  }
}
