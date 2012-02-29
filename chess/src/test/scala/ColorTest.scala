package lila.chess

class ColorTest extends ChessTest {

  "Color" should {
    "unary !" in {
      "white" in { !White must_== Black }
      "black" in { !Black must_== White }
    }
  }
}
