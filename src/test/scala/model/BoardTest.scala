package lila
package model

import Pos._

class BoardTest extends LilaSpec {

  val board = Board()

  "a board" should {

    "position pieces correctly" in {
      board.pieces must havePairs(A1 -> (White - Rook), B1 -> (White - Knight), C1 -> (White - Bishop), D1 -> (White - Queen), E1 -> (White - King), F1 -> (White - Bishop), G1 -> (White - Knight), H1 -> (White - Rook), A2 -> (White - Pawn), B2 -> (White - Pawn), C2 -> (White - Pawn), D2 -> (White - Pawn), E2 -> (White - Pawn), F2 -> (White - Pawn), G2 -> (White - Pawn), H2 -> (White - Pawn), A7 -> (Black - Pawn), B7 -> (Black - Pawn), C7 -> (Black - Pawn), D7 -> (Black - Pawn), E7 -> (Black - Pawn), F7 -> (Black - Pawn), G7 -> (Black - Pawn), H7 -> (Black - Pawn), A8 -> (Black - Rook), B8 -> (Black - Knight), C8 -> (Black - Bishop), D8 -> (Black - Queen), E8 -> (Black - King), F8 -> (Black - Bishop), G8 -> (Black - Knight), H8 -> (Black - Rook))
    }

    "have pieces by default" in {
      board.pieces must not beEmpty
    }

    "allow a piece to be placed" in {
      board place White - Rook at E3 must beSuccess.like {
        case b ⇒ b(E3) mustEqual Some(White - Rook)
      }
    }

    "allow a piece to be taken" in {
      board take A1 must beSuccess.like {
        case b ⇒ b(A1) must beNone
      }
    }

    "allow a piece to move" in {
      board move E2 to E4 must beSuccess.like {
        case b ⇒ b(E4) mustEqual Some(White - Pawn)
      }
    }

    "not allow an empty position to move" in {
      board move E5 to E6 must beFailure
    }

    "not allow a piece to move to an occupied position" in {
      board move A1 to A2 must beFailure
    }

    "allow a pawn to be promoted to any role" in {
      forall(Seq(Queen, Knight, Rook, Bishop)) { role ⇒
        Board.empty place Black - Pawn at A8 flatMap (_ promote A8 to role) must beSuccess.like {
          case b ⇒ b(A8) mustEqual Some(Black - role)
        }
      }
    }

    "not allow a pawn to be promoted to king or pawn" in {
      forall(Seq(King, Pawn)) { role ⇒
        Board.empty place Black - Pawn at A8 flatMap (_ promote A8 to role) must beFailure
      }
    }

    "not allow an empty position to be promoted" in {
      board promote A6 to Queen must beFailure
    }

    "not allow a non-pawn to be promoted" in {
      board promote A1 to Queen must beFailure
    }

    "allow chaining actions" in {
      Board.empty.seq(
        _ place White-Pawn at A2,
        _ place White-Pawn at A3,
        _ move A2 to A4
      ) must beSuccess.like {
        case b => b(A4) mustEqual Some(White-Pawn)
      }
    }

    "fail on bad actions chain" in {
      Board.empty.seq(
        _ place White-Pawn at A2,
        _ place White-Pawn at A3,
        _ move B2 to B4
      ) must beFailure
    }

    "provide occupation map" in {
      Board(
        A2 -> (White-Pawn),
        A3 -> (White-Pawn),
        D1 -> (White-King),
        E8 -> (Black-King),
        H4 -> (Black-Queen)
      ).occupation must havePairs(
        White -> Set(A2, A3, D1),
        Black -> Set(E8, H4)
      )
    }

    //"advise which positions currently threaten a given position, given a side (opposing pawns)" in {
    //val newBoard = board.place(White-Pawn).at(F6)
    //(newBoard threatsTo Black at G7) must containPositions(F6)
    //}

    //"advise which positions currently threaten a given position, given a side (opposing non-threatening piece)" in {
    //val newBoard = board.place(White-Knight).at(F6)
    //(newBoard threatsTo Black at G7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (multiple pawn opponents)" in {
    //val newBoard = board.place(White-Pawn).at(F6).place(White-Pawn).at(H6)
    //(newBoard threatsTo Black at G7) must containPositions(F6, H6)
    //}

    //"advise which positions currently threaten a given position, given a side (same side pawns)" in {
    //val newBoard = board.place(Black-Pawn).at(F6)
    //(newBoard threatsTo Black at G7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing rooks)" in {
    //val newBoard = board.place(White-Rook).at(B5)
    //(newBoard threatsTo Black at A5) must containPositions(B5)
    //(newBoard threatsTo Black at H5) must containPositions(B5)
    //(newBoard threatsTo Black at B8) must containPositions(B5)
    //(newBoard threatsTo Black at B1) must containPositions(B5)
    //}

    //"advise which positions currently threaten a given position, given a side (same side rooks)" in {
    //val newBoard = board.place(Black-Rook).at(B5)
    //(newBoard threatsTo Black at A5) must beEmpty
    //(newBoard threatsTo Black at H5) must beEmpty
    //(newBoard threatsTo Black at B8) must beEmpty
    //(newBoard threatsTo Black at B1) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed rook)" in {
    //val newBoard = board.place(White-Rook).at(B5).place(Black-Rook).at(D5)
    //(newBoard threatsTo Black at E5) must beEmpty
    //(newBoard threatsTo Black at D5) must containPositions(B5)
    //(newBoard threatsTo Black at C5) must containPositions(B5)
    //(newBoard threatsTo Black at B5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing bishops)" in {
    //val newBoard = board.place(Black-Bishop).at(F7)
    //(newBoard threatsTo White at G8) must containPositions(F7)
    //(newBoard threatsTo White at G6) must containPositions(F7)
    //(newBoard threatsTo White at E8) must containPositions(F7)
    //(newBoard threatsTo White at A2) must containPositions(F7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side bishops)" in {
    //val newBoard = board.place(Black-Bishop).at(D6)
    //(newBoard threatsTo Black at F4) must beEmpty
    //(newBoard threatsTo Black at F8) must beEmpty
    //(newBoard threatsTo Black at B8) must beEmpty
    //(newBoard threatsTo Black at B4) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed bishops)" in {
    //val newBoard = board.place(White-Rook).at(B5).place(Black-Bishop).at(D7)
    //(newBoard threatsTo White at A4) must beEmpty
    //(newBoard threatsTo White at B5) must containPositions(D7)
    //(newBoard threatsTo White at C6) must containPositions(D7)
    //(newBoard threatsTo White at D7) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing queens on rank/file)" in {
    //val newBoard = board.place(White-Queen).at(B5)
    //(newBoard threatsTo Black at A5) must containPositions(B5)
    //(newBoard threatsTo Black at H5) must containPositions(B5)
    //(newBoard threatsTo Black at B8) must containPositions(B5)
    //(newBoard threatsTo Black at B1) must containPositions(B5)
    //}

    //"advise which positions currently threaten a given position, given a side (same side queen on rank/file)" in {
    //val newBoard = board.place(Black-Queen).at(B5)
    //(newBoard threatsTo Black at A5) must beEmpty
    //(newBoard threatsTo Black at H5) must beEmpty
    //(newBoard threatsTo Black at B8) must beEmpty
    //(newBoard threatsTo Black at B1) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed queen on rank/file)" in {
    //val newBoard = board.place(White-Queen).at(B5).place(Black-Rook).at(D5)
    //(newBoard threatsTo Black at E5) must beEmpty
    //(newBoard threatsTo Black at D5) must containPositions(B5)
    //(newBoard threatsTo Black at C5) must containPositions(B5)
    //(newBoard threatsTo Black at B5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing queens on diagonal)" in {
    //val newBoard = board.place(Black-Queen).at(F7)
    //(newBoard threatsTo White at G8) must containPositions(F7)
    //(newBoard threatsTo White at G6) must containPositions(F7)
    //(newBoard threatsTo White at E8) must containPositions(F7)
    //(newBoard threatsTo White at A2) must containPositions(F7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side queen on diagonal)" in {
    //val newBoard = board.place(Black-Queen).at(D6)
    //(newBoard threatsTo Black at F4) must beEmpty
    //(newBoard threatsTo Black at F8) must beEmpty
    //(newBoard threatsTo Black at B8) must beEmpty
    //(newBoard threatsTo Black at B4) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (eclipsed queen on diagonal)" in {
    //val newBoard = board.place(White-Rook).at(B5).place(Black-Queen).at(D7)
    //(newBoard threatsTo White at A4) must beEmpty
    //(newBoard threatsTo White at B5) must containPositions(D7)
    //(newBoard threatsTo White at C6) must containPositions(D7)
    //(newBoard threatsTo White at D7) must beEmpty
    //}
    //}

    //"advise which positions currently threaten a given position, given a side (opposing knights)" in {
    //val newBoard = board.place(Black-Knight).at(F7)
    //(newBoard threatsTo White at D8) must containPositions(F7)
    //(newBoard threatsTo White at H8) must containPositions(F7)
    //(newBoard threatsTo White at D6) must containPositions(F7)
    //(newBoard threatsTo White at H6) must containPositions(F7)
    //(newBoard threatsTo White at E5) must containPositions(F7)
    //(newBoard threatsTo White at G5) must containPositions(F7)
    //}

    //"advise which positions currently threaten a given position, given a side (same side knights)" in {
    //val newBoard = board.place(Black-Knight).at(F7)
    //(newBoard threatsTo Black at D8) must beEmpty
    //(newBoard threatsTo Black at H8) must beEmpty
    //(newBoard threatsTo Black at D6) must beEmpty
    //(newBoard threatsTo Black at H6) must beEmpty
    //(newBoard threatsTo Black at E5) must beEmpty
    //(newBoard threatsTo Black at G5) must beEmpty
    //}

    //"advise which positions currently threaten a given position, given a side (opposing kings)" in {
    //val newBoard = board.place(Black-King).at(B4)
    //(newBoard threatsTo White at B3) must containPositions(B4)
    //(newBoard threatsTo White at B5) must containPositions(B4)
    //(newBoard threatsTo White at A3) must containPositions(B4)
    //(newBoard threatsTo White at A4) must containPositions(B4)
    //(newBoard threatsTo White at A5) must containPositions(B4)
    //(newBoard threatsTo White at C3) must containPositions(B4)
    //(newBoard threatsTo White at C4) must containPositions(B4)
    //(newBoard threatsTo White at C5) must containPositions(B4)
    //}

    //"advise which positions currently threaten a given position, given a side (same side kings)" in {
    //val newBoard = board.place(Black-King).at(A3)
    //(newBoard threatsTo Black at A4) must beEmpty
    //(newBoard threatsTo Black at A2) must beEmpty
    //(newBoard threatsTo Black at B2) must beEmpty
    //(newBoard threatsTo Black at B3) must beEmpty
    //(newBoard threatsTo Black at B4) must beEmpty
  }
}
