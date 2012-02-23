package lila
package model

import Pos._

class PosTest extends LilaSpec {

  "A position" should {

    "be used to derive a relative position on the board" in {
      "D5 ^ 1" in { D5 ^ 1 must_== D6.some }
      "D5 v 1" in { D5 v 1 must_== D4.some }
      "D5 > 1" in { D5 > 1 must_== E5.some }
      "D5 < 1" in { D5 < 1 must_== C5.some }
    }

    "be used to calculate a non-position off the edge of the board" in {
      "D5 ^ 4" in { D5 ^ 4 must beNone }
      "D5 v 5" in { D5 v 5 must beNone }
      "D5 > 4" in { D5 > 5 must beNone }
      "D5 < 5" in { D5 < 5 must beNone }
    }

    "be able to calculate a relative position with negative numbers" in {
      "D5 ^ -2" in { D5 ^ -2 must_== D3.some }
      "D5 v -3" in { D5 v -3 must_== D8.some }
      "D5 > -1" in { D5 > -1 must_== C5.some }
      "D5 < -3" in { D5 < -3 must_== G5.some }
    }

    "be used to calculate a non-position off the edge of the board using negative numbers" in {
      "D5 ^ -6" in { D5 ^ -6 must beNone }
      "D5 v -6" in { D5 v -6 must beNone }
      "D5 > -6" in { D5 > -6 must beNone }
      "D5 < -6" in { D5 < -6 must beNone }
    }

    "be used to derive a relative list of positions" in {
      "D4 ^^ 3" in { D4 ^^ 3 must contain(D4, D5, D6, D7) }
      "D4 >> 3" in { D4 >> 3 must contain(D4, E4, F4, G4) }
      "D4 vv 3" in { D4 vv 3 must contain(D4, D3, D2, D1) }
      "D4 << 3" in { D4 << 3 must contain(D4, C4, B4, A4) }
    }

    "be used to derive a relative list of positions not including those off the board" in {
      "D4 ^^ 8" in { D4 ^^ 8 must contain(D4, D5, D6, D7, D8) }
      "D4 >> 8" in { D4 >> 8 must contain(D4, E4, F4, G4, H4) }
      "D4 vv 8" in { D4 vv 8 must contain(D4, D3, D2, D1) }
      "D4 << 8" in { D4 << 8 must contain(D4, C4, B4, A4) }
    }
  }
}
