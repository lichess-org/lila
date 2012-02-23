package lila
package model

class ColorTest extends LilaSpec {

  "Color" should {
    "opposite" in {
      "white" in { !White must_== Black }
      "black" in { !Black must_== White }
    }
  }
}
