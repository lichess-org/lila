package shogi

import scalaz.Validation.FlatMap._
import Pos._
import variant._

class VariantTest extends ShogiTest {

  val board = makeBoard

  "standard" should {

    "position pieces correctly" in {
      Standard.pieces must havePairs(
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
  }
}
