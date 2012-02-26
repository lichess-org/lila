package lila.chess
package model

class ColorTest extends LilaSpec {

  "Color" should {
    "unary !" in {
      "white" in { !White must_== Black }
      "black" in { !Black must_== White }
    }
  }
}
