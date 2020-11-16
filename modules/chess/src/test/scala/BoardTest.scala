package chess

import Pos._

class BoardTest extends ChessTest {

  val board = makeBoard

  "a board" should {

    "position pieces correctly" in {
      board.pieces must havePairs(
        A1 -> (White - Rook),
        B1 -> (White - Knight),
        C1 -> (White - Bishop),
        D1 -> (White - Queen),
        E1 -> (White - King),
        F1 -> (White - Bishop),
        G1 -> (White - Knight),
        H1 -> (White - Rook),
        A2 -> (White - Pawn),
        B2 -> (White - Pawn),
        C2 -> (White - Pawn),
        D2 -> (White - Pawn),
        E2 -> (White - Pawn),
        F2 -> (White - Pawn),
        G2 -> (White - Pawn),
        H2 -> (White - Pawn),
        A7 -> (Black - Pawn),
        B7 -> (Black - Pawn),
        C7 -> (Black - Pawn),
        D7 -> (Black - Pawn),
        E7 -> (Black - Pawn),
        F7 -> (Black - Pawn),
        G7 -> (Black - Pawn),
        H7 -> (Black - Pawn),
        A8 -> (Black - Rook),
        B8 -> (Black - Knight),
        C8 -> (Black - Bishop),
        D8 -> (Black - Queen),
        E8 -> (Black - King),
        F8 -> (Black - Bishop),
        G8 -> (Black - Knight),
        H8 -> (Black - Rook)
      )
    }

    "have pieces by default" in {
      board.pieces must not beEmpty
    }

    "have castling rights by default" in {
      board.history.castles == Castles.all
    }

    "allow a piece to be placed" in {
      board place White - Rook at E3 must beSuccess.like {
        case b => b(E3) mustEqual Some(White - Rook)
      }
    }

    "allow a piece to be taken" in {
      board take A1 must beSome.like {
        case b => b(A1) must beNone
      }
    }

    "allow a piece to move" in {
      board move E2 to E4 must beSuccess.like {
        case b => b(E4) mustEqual Some(White - Pawn)
      }
    }

    "not allow an empty position to move" in {
      board move E5 to E6 must beFailure
    }

    "not allow a piece to move to an occupied position" in {
      board move A1 to A2 must beFailure
    }

    "allow a pawn to be promoted to a queen" in {
      makeEmptyBoard.place(Black.pawn, A8) flatMap (_ promote A8) must beSome.like {
        case b => b(A8) must beSome(Black.queen)
      }
    }

    "allow chaining actions" in {
      makeEmptyBoard.seq(
        _ place White - Pawn at A2,
        _ place White - Pawn at A3,
        _ move A2 to A4
      ) must beSuccess.like {
        case b => b(A4) mustEqual Some(White - Pawn)
      }
    }

    "fail on bad actions chain" in {
      makeEmptyBoard.seq(
        _ place White - Pawn at A2,
        _ place White - Pawn at A3,
        _ move B2 to B4
      ) must beFailure
    }

    "provide occupation map" in {
      makeBoard(
        A2 -> (White - Pawn),
        A3 -> (White - Pawn),
        D1 -> (White - King),
        E8 -> (Black - King),
        H4 -> (Black - Queen)
      ).occupation must_== Color.Map(
        white = Set(A2, A3, D1),
        black = Set(E8, H4)
      )
    }

    "navigate in pos based on pieces" in {
      "right to end" in {
        val board: Board = """
R   K  R"""
        E1 >| (p => board.pieces contains p) must_== List(F1, G1, H1)
      }
      "right to next" in {
        val board: Board = """
R   KB R"""
        E1 >| (p => board.pieces contains p) must_== List(F1)
      }
      "left to end" in {
        val board: Board = """
R   K  R"""
        E1 |< (p => board.pieces contains p) must_== List(D1, C1, B1, A1)
      }
      "right to next" in {
        val board: Board = """
R  BK  R"""
        E1 |< (p => board.pieces contains p) must_== List(D1)
      }
    }
  }
}
