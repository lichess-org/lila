package lila.chess

class PieceTest extends ChessTest {

  "Piece" should {
    "compare" in {
      "objects and - method" in {
        !White - Pawn must_== Black - Pawn
      }
      "value and - method" in {
        val color = White
        !color-Pawn must_== Black - Pawn
      }
    }
  }
}
