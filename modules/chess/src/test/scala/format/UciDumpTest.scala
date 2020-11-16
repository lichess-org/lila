package chess
package format

class UciDumpTest extends ChessTest {

  import pgn.Fixtures._

  "only raw moves" should {
    "empty" in {
      UciDump(Nil, None, variant.Standard) must beSuccess.like {
        case x => x must beEmpty
      }
    }
    "simple" in {
      UciDump(simple.split(' ').toList, None, variant.Standard) must beSuccess.like {
        case moves => moves must_== "e2e3 b8c6 d2d4 g8f6".split(" ").toList
      }
    }
    "complete" in {
      UciDump(fromProd2.split(' ').toList, None, variant.Standard) must beSuccess.like {
        case moves =>
          moves must_== List(
            "e2e4",
            "c7c5",
            "g1f3",
            "b8c6",
            "d2d4",
            "c5d4",
            "f3d4",
            "g8f6",
            "b1c3",
            "e7e5",
            "d4f5",
            "d7d5",
            "g2g4",
            "c8f5",
            "e4f5",
            "d5d4",
            "c3e2",
            "f6g4",
            "f1h3",
            "d8d5",
            "e2g3",
            "f8b4",
            "c1d2",
            "b4d2",
            "d1d2",
            "d5f3",
            "d2e2",
            "f3e2",
            "e1e2",
            "g4h6",
            "h3g2",
            "e8f8",
            "h1e1",
            "f8e7",
            "e2d3",
            "c6b4",
            "d3d2",
            "h8c8",
            "c2c3",
            "d4c3",
            "b2c3",
            "c8d8",
            "d2c1",
            "b4d3",
            "c1c2",
            "d3e1",
            "a1e1",
            "e7f6",
            "g2e4",
            "d8b8",
            "e1d1",
            "f6g5",
            "d1d7",
            "a7a6",
            "e4b7",
            "a8a7",
            "b7c6",
            "a7d7",
            "c6d7",
            "b8d8",
            "d7c6",
            "h6f5",
            "g3f5",
            "g5f5",
            "f2f3",
            "d8b8",
            "c6e4",
            "f5f4",
            "c3c4",
            "b8h8",
            "c4c5",
            "a6a5",
            "c5c6",
            "f7f5",
            "e4d5",
            "h8c8",
            "c2c3",
            "g7g6",
            "c3c4",
            "h7h5",
            "c4b5",
            "c8b8",
            "b5a6",
            "a5a4",
            "c6c7",
            "b8c8",
            "a6b7",
            "c8h8",
            "c7c8q",
            "h8c8",
            "b7c8",
            "g6g5",
            "c8d7",
            "g5g4",
            "f3g4",
            "h5g4",
            "d7e6",
            "e5e4",
            "d5c4",
            "f4g5",
            "e6e5",
            "e4e3",
            "c4d3",
            "f5f4",
            "e5e4",
            "g4g3",
            "e4f3",
            "g3h2",
            "f3g2",
            "f4f3",
            "g2h2",
            "e3e2",
            "d3e2",
            "f3e2",
            "h2h3",
            "e2e1q",
            "h3g2",
            "e1e6"
          )
      }
    }
    // "960" in {
    //   UciDump(complete960, None, Variant.Chess960) must beSuccess.like {
    //     case moves => moves must_== "e2e3 e8f6 f1g3 f8e6 e1f3 d7d5 f3d4 e6d4 e3d4 e7e6 d1e1 f6g4 e1e2 f7f6 c2c4 d5c4 b1e4 d8d4 e4f3 g4e5 g3e4 e5f3 g2f3 g8f7 e4f6 g7f6 c1d1 e6e5 h2h4 f7g6 g1h2 g6f5 e2e5 f6e5 h2e5 h8e5 h1f1 e5f4 d2d3 d4d3 d1e1 f4d2".split(" ").toList
    //   }
    // }
  }
}
