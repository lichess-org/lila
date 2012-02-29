package lila.chess

import Pos._

class PosTest extends ChessTest {

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
      "D4 >| false" in { D4 >| (_ ⇒ false) must contain(E4, F4, G4, H4) }
      "D4 |< false" in { D4 |< (_ ⇒ false) must contain(C4, B4, A4) }
      "D4 >| (==G4)" in { D4 >| (G4 ==) must contain(E4, F4, G4) }
      "D4 |< (==C4)" in { D4 |< (C4 ==) must contain(C4) }
    }

    "be a string" in {
      "D5" in { D5.toString must_== "d5" }
    }
  }
}
