package lila
package model

import Pos._

object BoardTest extends LilaSpec {

  val board = Board()

  "a board" should {

    "position pieces correctly" in {
      board.pieces must havePairs(
        Pos('a1) -> (White - Rook),
        Pos('b1) -> (White - Knight),
        Pos('c1) -> (White - Bishop),
        Pos('d1) -> (White - Queen),
        Pos('e1) -> (White - King),
        Pos('f1) -> (White - Bishop),
        Pos('g1) -> (White - Knight),
        Pos('h1) -> (White - Rook),
        Pos('a2) -> (White - Pawn),
        Pos('b2) -> (White - Pawn),
        Pos('c2) -> (White - Pawn),
        Pos('d2) -> (White - Pawn),
        Pos('e2) -> (White - Pawn),
        Pos('f2) -> (White - Pawn),
        Pos('g2) -> (White - Pawn),
        Pos('h2) -> (White - Pawn),
        Pos('a7) -> (Black - Pawn),
        Pos('b7) -> (Black - Pawn),
        Pos('c7) -> (Black - Pawn),
        Pos('d7) -> (Black - Pawn),
        Pos('e7) -> (Black - Pawn),
        Pos('f7) -> (Black - Pawn),
        Pos('g7) -> (Black - Pawn),
        Pos('h7) -> (Black - Pawn),
        Pos('a8) -> (Black - Rook),
        Pos('b8) -> (Black - Knight),
        Pos('c8) -> (Black - Bishop),
        Pos('d8) -> (Black - Queen),
        Pos('e8) -> (Black - King),
        Pos('f8) -> (Black - Bishop),
        Pos('g8) -> (Black - Knight),
        Pos('h8) -> (Black - Rook)
      )
    }

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

    "allow a piece to move" in {
      board move 'e2 to 'e4 must beSuccess.like {
        case b ⇒ b('e4) mustEqual Some(White - Pawn)
      }
    }

    "not allow an empty position to move" in {
      board move 'e5 to 'e6 must beFailure
    }

    "not allow a piece to move to an occupied position" in {
      board move 'a1 to 'a2 must beFailure
    }

    //"allow a pawn to be promoted to a queen" in {
    //var updatedBoard = board place Black-Pawn at 'a8
    //updatedBoard = updatedBoard promote position('a8) to Queen
    //updatedBoard.pieces must_== Map(position('a8) -> Black-Queen)
    //}

    //"allow a pawn to be promoted to a bishop" in {
    //var updatedBoard = board place Black-Pawn at 'f8
    //updatedBoard = updatedBoard promote position('f8) to Bishop
    //updatedBoard.pieces must_== Map(position('f8) -> Black-Bishop)
    //}

    //"allow a pawn to be promoted to a knight" in {
    //var updatedBoard = board place White-Pawn at 'g1
    //updatedBoard = updatedBoard promote position('g1) to Knight
    //updatedBoard.pieces must_== Map(position('g1) -> White-Knight)
    //}

    //"allow a pawn to be promoted to a rook" in {
    //var updatedBoard = board place White-Pawn at 'd1
    //updatedBoard = updatedBoard promote position('d1) to Rook
    //updatedBoard.pieces must_== Map(position('d1) -> White-Rook)
    //}

    //"no-op when promoting a pawn to a pawn" in {
    //var updatedBoard = board place White-Pawn at 'd2
    //updatedBoard = updatedBoard promote position('d2) to Pawn
    //updatedBoard.pieces must_== Map(position('d2) -> White-Pawn)
    //}

    //"not allow a pawn to be promoted to a king" in {
    //val updatedBoard = board place White-Pawn at 'd2
    //(updatedBoard promote position('d2) to King) must throwAn[IllegalPromotionException]
    //}

    //"not allow an empty position to be promoted" in {
    //(board promote position('a6) to Queen) must throwAn[IllegalPromotionException]
    //}

    //"not allow a non-pawn to be promoted" in {
    //val updatedBoard = board place Black-Knight at 'f7
    //(updatedBoard promote position('f7) to Queen) must throwAn[IllegalPromotionException]
    //}

    //"advise which positions currently threaten a given position, given a side (opposing pawns)" in {
    //val newBoard = board.place(White-Pawn).at(position('f6))
    //(newBoard threatsTo Black at 'g7) must containPositions('f6)
    //}

    //"advise which positions currently threaten a given position, given a side (opposing non-threatening piece)" in {
    //val newBoard = board.place(White-Knight).at(position('f6))
    //(newBoard threatsTo Black at 'g7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (multiple pawn opponents)" in {
    //val newBoard = board.place(White-Pawn).at(position('f6)).place(White-Pawn).at(position('h6))
    //(newBoard threatsTo Black at 'g7) must containPositions('f6, 'h6)
    //}

    //"advise which positions currently threaten a given position, given a side (same side pawns)" in {
    //val newBoard = board.place(Black-Pawn).at(position('f6))
    //(newBoard threatsTo Black at 'g7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing rooks)" in {
    //val newBoard = board.place(White-Rook).at(position('b5))
    //(newBoard threatsTo Black at 'a5) must containPositions('b5)
    //(newBoard threatsTo Black at 'h5) must containPositions('b5)
    //(newBoard threatsTo Black at 'b8) must containPositions('b5)
    //(newBoard threatsTo Black at 'b1) must containPositions('b5)
    //}

    //"advise which positions currently threaten a given position, given a side (same side rooks)" in {
    //val newBoard = board.place(Black-Rook).at(position('b5))
    //(newBoard threatsTo Black at 'a5) must beEmpty
    //(newBoard threatsTo Black at 'h5) must beEmpty
    //(newBoard threatsTo Black at 'b8) must beEmpty
    //(newBoard threatsTo Black at 'b1) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed rook)" in {
    //val newBoard = board.place(White-Rook).at(position('b5)).place(Black-Rook).at(position('d5))
    //(newBoard threatsTo Black at 'e5) must beEmpty
    //(newBoard threatsTo Black at 'd5) must containPositions('b5)
    //(newBoard threatsTo Black at 'c5) must containPositions('b5)
    //(newBoard threatsTo Black at 'b5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing bishops)" in {
    //val newBoard = board.place(Black-Bishop).at(position('f7))
    //(newBoard threatsTo White at 'g8) must containPositions('f7)
    //(newBoard threatsTo White at 'g6) must containPositions('f7)
    //(newBoard threatsTo White at 'e8) must containPositions('f7)
    //(newBoard threatsTo White at 'a2) must containPositions('f7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side bishops)" in {
    //val newBoard = board.place(Black-Bishop).at(position('d6))
    //(newBoard threatsTo Black at 'f4) must beEmpty
    //(newBoard threatsTo Black at 'f8) must beEmpty
    //(newBoard threatsTo Black at 'b8) must beEmpty
    //(newBoard threatsTo Black at 'b4) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed bishops)" in {
    //val newBoard = board.place(White-Rook).at(position('b5)).place(Black-Bishop).at(position('d7))
    //(newBoard threatsTo White at 'a4) must beEmpty
    //(newBoard threatsTo White at 'b5) must containPositions('d7)
    //(newBoard threatsTo White at 'c6) must containPositions('d7)
    //(newBoard threatsTo White at 'd7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing queens on rank/file)" in {
    //val newBoard = board.place(White-Queen).at(position('b5))
    //(newBoard threatsTo Black at 'a5) must containPositions('b5)
    //(newBoard threatsTo Black at 'h5) must containPositions('b5)
    //(newBoard threatsTo Black at 'b8) must containPositions('b5)
    //(newBoard threatsTo Black at 'b1) must containPositions('b5)
    //}

    //"advise which positions currently threaten a given position, given a side (same side queen on rank/file)" in {
    //val newBoard = board.place(Black-Queen).at(position('b5))
    //(newBoard threatsTo Black at 'a5) must beEmpty
    //(newBoard threatsTo Black at 'h5) must beEmpty
    //(newBoard threatsTo Black at 'b8) must beEmpty
    //(newBoard threatsTo Black at 'b1) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed queen on rank/file)" in {
    //val newBoard = board.place(White-Queen).at(position('b5)).place(Black-Rook).at(position('d5))
    //(newBoard threatsTo Black at 'e5) must beEmpty
    //(newBoard threatsTo Black at 'd5) must containPositions('b5)
    //(newBoard threatsTo Black at 'c5) must containPositions('b5)
    //(newBoard threatsTo Black at 'b5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing queens on diagonal)" in {
    //val newBoard = board.place(Black-Queen).at(position('f7))
    //(newBoard threatsTo White at 'g8) must containPositions('f7)
    //(newBoard threatsTo White at 'g6) must containPositions('f7)
    //(newBoard threatsTo White at 'e8) must containPositions('f7)
    //(newBoard threatsTo White at 'a2) must containPositions('f7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side queen on diagonal)" in {
    //val newBoard = board.place(Black-Queen).at(position('d6))
    //(newBoard threatsTo Black at 'f4) must beEmpty
    //(newBoard threatsTo Black at 'f8) must beEmpty
    //(newBoard threatsTo Black at 'b8) must beEmpty
    //(newBoard threatsTo Black at 'b4) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed queen on diagonal)" in {
    //val newBoard = board.place(White-Rook).at(position('b5)).place(Black-Queen).at(position('d7))
    //(newBoard threatsTo White at 'a4) must beEmpty
    //(newBoard threatsTo White at 'b5) must containPositions('d7)
    //(newBoard threatsTo White at 'c6) must containPositions('d7)
    //(newBoard threatsTo White at 'd7) must beEmpty
    //}
    //}

    //"advise which positions currently threaten a given position, given a side (opposing knights)" in {
    //val newBoard = board.place(Black-Knight).at(position('f7))
    //(newBoard threatsTo White at 'd8) must containPositions('f7)
    //(newBoard threatsTo White at 'h8) must containPositions('f7)
    //(newBoard threatsTo White at 'd6) must containPositions('f7)
    //(newBoard threatsTo White at 'h6) must containPositions('f7)
    //(newBoard threatsTo White at 'e5) must containPositions('f7)
    //(newBoard threatsTo White at 'g5) must containPositions('f7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side knights)" in {
    //val newBoard = board.place(Black-Knight).at(position('f7))
    //(newBoard threatsTo Black at 'd8) must beEmpty
    //(newBoard threatsTo Black at 'h8) must beEmpty
    //(newBoard threatsTo Black at 'd6) must beEmpty
    //(newBoard threatsTo Black at 'h6) must beEmpty
    //(newBoard threatsTo Black at 'e5) must beEmpty
    //(newBoard threatsTo Black at 'g5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing kings)" in {
    //val newBoard = board.place(Black-King).at(position('b4))
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
    //val newBoard = board.place(Black-King).at(position('a3))
    //(newBoard threatsTo Black at 'a4) must beEmpty
    //(newBoard threatsTo Black at 'a2) must beEmpty
    //(newBoard threatsTo Black at 'b2) must beEmpty
    //(newBoard threatsTo Black at 'b3) must beEmpty
    //(newBoard threatsTo Black at 'b4) must beEmpty
  }
}
