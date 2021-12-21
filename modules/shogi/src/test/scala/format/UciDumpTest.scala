package shogi
package format

class UsiDumpTest extends ShogiTest {

  import pgn.Fixtures._

  "only raw moves" should {
    "empty" in {
      UsiDump(Nil, None, variant.Standard) must beValid.like { case x =>
        x must beEmpty
      }
    }
    // Pc4 Pb6 Pb4 Gd9e8
    "simple" in {
      UsiDump(simple.split(' ').toList, None, variant.Standard) must beValid.like { case moves =>
        moves must_== "7g7f 8c8d 8g8f 6a5b".split(" ").toList
      }
    }
    "complete" in {
      UsiDump(fromProd2.split(' ').toList, None, variant.Standard) must beValid.like { case moves =>
        moves must_== List(
      "7g7f",  "8c8d",  "8i7g",  "8d8e", "7g8e",
      "8b8e",  "9g9f",  "8e8g+", "8h6f", "8g8i",
      "7i6h",  "8i9i",  "5i4h",  "9i9f", "6f3c+",
      "2b3c",  "2g2f",  "9f7f",  "4g4f", "7f4f",
      "4h5h",  "4f3g",  "2i3g",  "3c5e", "2h4h",
      "5e3g+", "4h4c+", "4a5b",  "4c2c", "P*2b",
      "2c2e",  "3g1i",  "R*2g",  "1i1h", "2g4g",
      "B*2g",  "4g4h",  "1h1g",  "2e5e", "2g3f+",
      "5h5i",  "N*4g",  "4h4g",  "3f4g", "3i4h",
      "4g3f",  "6h7g",  "R*9i",  "N*2i", "3f6i"
)
      }
    }
  }
}
