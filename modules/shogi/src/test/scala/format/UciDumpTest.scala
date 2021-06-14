package shogi
package format

class UciDumpTest extends ShogiTest {

  import pgn.Fixtures._

  "only raw moves" should {
    "empty" in {
      UciDump(Nil, None, variant.Standard) must beSuccess.like { case x =>
        x must beEmpty
      }
    }
    // Pc4 Pb6 Pb4 Gd9e8
    "simple" in {
      UciDump(simple.split(' ').toList, None, variant.Standard) must beSuccess.like { case moves =>
        moves must_== "c3c4 b7b6 b3b4 d9e8".split(" ").toList
      }
    }
    "complete" in {
      UciDump(fromProd2.split(' ').toList, None, variant.Standard) must beSuccess.like { case moves =>
        moves must_== List(
          "c3c4",
          "b7b6",
          "b1c3",
          "b6b5",
          "c3b5",
          "b8b5",
          "a3a4",
          "b5b3+",
          "b2d4",
          "b3b1",
          "c1d2",
          "b1a1",
          "e1f2",
          "a1a4",
          "d4g7+",
          "h8g7",
          "h3h4",
          "a4c4",
          "f3f4",
          "c4f4",
          "f2e2",
          "f4g3",
          "h1g3",
          "g7e5",
          "h2f2",
          "e5g3+",
          "f2f7+",
          "f9e8",
          "f7h7",
          "P*h8",
          "h7h5",
          "g3i1",
          "R*h3",
          "i1i2",
          "h3f3",
          "B*h3",
          "f3f2",
          "i2i3",
          "h5e5",
          "h3g4+",
          "e2e1",
          "N*f3",
          "f2f3",
          "g4f3",
          "g1f2",
          "f3g4",
          "d2c3",
          "R*a1",
          "N*h1",
          "g4d1"
        )
      }
    }
  }
}
