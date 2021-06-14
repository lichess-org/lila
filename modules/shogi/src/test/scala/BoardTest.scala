package shogi

import Pos._

class BoardTest extends ShogiTest {

  val board = makeBoard

  "a board" should {

    "position pieces correctly" in {
      board.pieces must havePairs(
        A1 -> (Sente - Lance),
        B1 -> (Sente - Knight),
        C1 -> (Sente - Silver),
        D1 -> (Sente - Gold),
        E1 -> (Sente - King),
        F1 -> (Sente - Gold),
        G1 -> (Sente - Silver),
        H1 -> (Sente - Knight),
        I1 -> (Sente - Lance),
        B2 -> (Sente - Bishop),
        H2 -> (Sente - Rook),
        A3 -> (Sente - Pawn),
        B3 -> (Sente - Pawn),
        C3 -> (Sente - Pawn),
        D3 -> (Sente - Pawn),
        E3 -> (Sente - Pawn),
        F3 -> (Sente - Pawn),
        G3 -> (Sente - Pawn),
        H3 -> (Sente - Pawn),
        I3 -> (Sente - Pawn),
        A7 -> (Gote - Pawn),
        B7 -> (Gote - Pawn),
        C7 -> (Gote - Pawn),
        D7 -> (Gote - Pawn),
        E7 -> (Gote - Pawn),
        F7 -> (Gote - Pawn),
        G7 -> (Gote - Pawn),
        H7 -> (Gote - Pawn),
        I7 -> (Gote - Pawn),
        B8 -> (Gote - Rook),
        H8 -> (Gote - Bishop),
        A9 -> (Gote - Lance),
        B9 -> (Gote - Knight),
        C9 -> (Gote - Silver),
        D9 -> (Gote - Gold),
        E9 -> (Gote - King),
        F9 -> (Gote - Gold),
        G9 -> (Gote - Silver),
        H9 -> (Gote - Knight),
        I9 -> (Gote - Lance)
      )
    }

    "have pieces by default" in {
      board.pieces must not beEmpty
    }

    "allow a piece to be placed" in {
      board.place(Sente - Rook, E5) must beSome.like { case b =>
        b(E5) mustEqual Some(Sente - Rook)
      }
    }

    "allow a piece to be taken" in {
      board take A1 must beSome.like { case b =>
        b(A1) must beNone
      }
    }

    "allow a piece to move" in {
      board.move(E3, E4) must beSome.like { case b =>
        b(E4) mustEqual Some(Sente - Pawn)
      }
    }

    "not allow an empty position to move" in {
      board.move(E5, E6) must beNone
    }

    "not allow a piece to move to an occupied position" in {
      board.move(A1, A3) must beNone
    }

    "allow a pawn to be promoted" in {
      makeEmptyBoard.place(Gote.pawn, A7) flatMap (_ promote A7) must beSome.like { case b =>
        b(A7) must beSome(Gote.tokin)
      }
    }

    "allow chaining actions" in {
      makeEmptyBoard.seq(
        _.place(Sente - Pawn, A2),
        _.place(Sente - Pawn, A3),
        _.move(A2, A4)
      ) must beSome.like { case b =>
        b(A4) mustEqual Some(Sente - Pawn)
      }
    }

    "fail on bad actions chain" in {
      makeEmptyBoard.seq(
        _.place(Sente - Pawn, A2),
        _.place(Sente - Pawn, C3),
        _.move(B3, B4)
      ) must beNone
    }

    "provide occupation map" in {
      makeBoard(
        A2 -> (Sente - Pawn),
        A3 -> (Sente - Pawn),
        D1 -> (Sente - King),
        E8 -> (Gote - King),
        H4 -> (Gote - Rook)
      ).occupation must_== Color.Map(
        sente = Set(A2, A3, D1),
        gote = Set(E8, H4)
      )
    }

    "navigate in pos based on pieces" in {
      "right to end" in {
        val board: Board = """
R   K   R"""
        E1 >| (p => board.pieces contains p) must_== List(F1, G1, H1, I1)
      }
      "right to next" in {
        val board: Board = """
R   KB  R"""
        E1 >| (p => board.pieces contains p) must_== List(F1)
      }
      "left to end" in {
        val board: Board = """
R   K   R"""
        E1 |< (p => board.pieces contains p) must_== List(D1, C1, B1, A1)
      }
      "right to next" in {
        val board: Board = """
R  BK   R"""
        E1 |< (p => board.pieces contains p) must_== List(D1)
      }
    }
  }
}
