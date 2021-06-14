package shogi

class PieceTest extends ShogiTest {

  "Piece" should {
    "compare" in {
      "objects and - method" in {
        !Sente - Pawn must_== Gote - Pawn
      }
      "value and - method" in {
        val color = Sente
        !color - Pawn must_== Gote - Pawn
      }
    }
  }
}
