package shogi

import Pos._

class PosTest extends ShogiTest {

  "A position" should {

    "be used to derive a relative list of positions" in {
      "SQ6F >| false" in { SQ6F >| (_ => false) must contain(SQ5F, SQ4F, SQ3F, SQ2F, SQ1F) }
      "SQ6F |< false" in { SQ6F |< (_ => false) must contain(SQ7F, SQ8F, SQ9F) }
      "SQ6F >| (==SQ3F)" in { SQ6F >| (SQ3F ==) must contain(SQ5F, SQ4F, SQ3F) }
      "SQ6F |< (==SQ7F)" in { SQ6F |< (SQ7F ==) must contain(SQ7F) }
    }

    "be a string" in {
      "SQ6E" in { SQ6E.toString must_== "d5" }
    }
  }
  "USI/UCI" should {
    "be correctly converted" in {
      SQ9I.usiKey must_== "9i"
      SQ9A.usiKey must_== "9a"
      SQ1A.usiKey must_== "1a"
      SQ1I.usiKey must_== "1i"
      SQ5E.usiKey must_== "5e"
      SQ5G.usiKey must_== "5g"
      SQ5C.usiKey must_== "5c"
      SQ6E.usiKey must_== "6e"
      SQ4E.usiKey must_== "4e"
    }
  }
}
