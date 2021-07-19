package shogi

import Pos._

class PosTest extends ShogiTest {

  "A position" should {

    "be used to derive a relative list of positions" in {
      "D4 >| false" in { D4 >| (_ => false) must contain(E4, F4, G4, H4, I4) }
      "D4 |< false" in { D4 |< (_ => false) must contain(C4, B4, A4) }
      "D4 >| (==G4)" in { D4 >| (G4 ==) must contain(E4, F4, G4) }
      "D4 |< (==C4)" in { D4 |< (C4 ==) must contain(C4) }
    }

    "be a string" in {
      "D5" in { D5.toString must_== "d5" }
    }
  }
  "USI/UCI" should {
    "be correctly converted" in {
      A1.usiKey must_== "9i"
      A9.usiKey must_== "9a"
      I9.usiKey must_== "1a"
      I1.usiKey must_== "1i"
      E5.usiKey must_== "5e"
      E3.usiKey must_== "5g"
      E7.usiKey must_== "5c"
      D5.usiKey must_== "6e"
      F5.usiKey must_== "4e"
    }
  }
}
