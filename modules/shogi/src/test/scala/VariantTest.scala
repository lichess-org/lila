package shogi

import cats.syntax.option._
import Pos._
import variant._

class VariantTest extends ShogiTest {

  "standard" should {

    "position pieces correctly" in {
      Standard.pieces must havePairs(
        SQ9I -> (Sente - Lance),
        SQ8I -> (Sente - Knight),
        SQ7I -> (Sente - Silver),
        SQ6I -> (Sente - Gold),
        SQ5I -> (Sente - King),
        SQ4I -> (Sente - Gold),
        SQ3I -> (Sente - Silver),
        SQ2I -> (Sente - Knight),
        SQ1I -> (Sente - Lance),
        SQ8H -> (Sente - Bishop),
        SQ2H -> (Sente - Rook),
        SQ9G -> (Sente - Pawn),
        SQ8G -> (Sente - Pawn),
        SQ7G -> (Sente - Pawn),
        SQ6G -> (Sente - Pawn),
        SQ5G -> (Sente - Pawn),
        SQ4G -> (Sente - Pawn),
        SQ3G -> (Sente - Pawn),
        SQ2G -> (Sente - Pawn),
        SQ1G -> (Sente - Pawn),
        SQ9C -> (Gote - Pawn),
        SQ8C -> (Gote - Pawn),
        SQ7C -> (Gote - Pawn),
        SQ6C -> (Gote - Pawn),
        SQ5C -> (Gote - Pawn),
        SQ4C -> (Gote - Pawn),
        SQ3C -> (Gote - Pawn),
        SQ2C -> (Gote - Pawn),
        SQ1C -> (Gote - Pawn),
        SQ8B -> (Gote - Rook),
        SQ2B -> (Gote - Bishop),
        SQ9A -> (Gote - Lance),
        SQ8A -> (Gote - Knight),
        SQ7A -> (Gote - Silver),
        SQ6A -> (Gote - Gold),
        SQ5A -> (Gote - King),
        SQ4A -> (Gote - Gold),
        SQ3A -> (Gote - Silver),
        SQ2A -> (Gote - Knight),
        SQ1A -> (Gote - Lance)
      )
    }
  }
}
