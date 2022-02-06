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
      "SQ6E" in { SQ6E.toString must_== "6e" }
    }
    "USI" in {
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

    "board sizes" in {
      "9x9" in {
        Pos.all must have size 81
        Pos.all must contain(
          exactly(
            SQ9I,
            SQ8I,
            SQ7I,
            SQ6I,
            SQ5I,
            SQ4I,
            SQ3I,
            SQ2I,
            SQ1I,
            SQ9H,
            SQ8H,
            SQ7H,
            SQ6H,
            SQ5H,
            SQ4H,
            SQ3H,
            SQ2H,
            SQ1H,
            SQ9G,
            SQ8G,
            SQ7G,
            SQ6G,
            SQ5G,
            SQ4G,
            SQ3G,
            SQ2G,
            SQ1G,
            SQ9F,
            SQ8F,
            SQ7F,
            SQ6F,
            SQ5F,
            SQ4F,
            SQ3F,
            SQ2F,
            SQ1F,
            SQ9E,
            SQ8E,
            SQ7E,
            SQ6E,
            SQ5E,
            SQ4E,
            SQ3E,
            SQ2E,
            SQ1E,
            SQ9D,
            SQ8D,
            SQ7D,
            SQ6D,
            SQ5D,
            SQ4D,
            SQ3D,
            SQ2D,
            SQ1D,
            SQ9C,
            SQ8C,
            SQ7C,
            SQ6C,
            SQ5C,
            SQ4C,
            SQ3C,
            SQ2C,
            SQ1C,
            SQ9B,
            SQ8B,
            SQ7B,
            SQ6B,
            SQ5B,
            SQ4B,
            SQ3B,
            SQ2B,
            SQ1B,
            SQ9A,
            SQ8A,
            SQ7A,
            SQ6A,
            SQ5A,
            SQ4A,
            SQ3A,
            SQ2A,
            SQ1A
          )
        )
      }
      "5x5" in {
        (SQ5E upTo SQ1A).toList must have size 25
        (SQ5E upTo SQ1A).toList must contain(
          exactly(
            SQ5E,
            SQ4E,
            SQ3E,
            SQ2E,
            SQ1E,
            SQ5D,
            SQ4D,
            SQ3D,
            SQ2D,
            SQ1D,
            SQ5C,
            SQ4C,
            SQ3C,
            SQ2C,
            SQ1C,
            SQ5B,
            SQ4B,
            SQ3B,
            SQ2B,
            SQ1B,
            SQ5A,
            SQ4A,
            SQ3A,
            SQ2A,
            SQ1A
          )
        )
      }
    }
  }
}
