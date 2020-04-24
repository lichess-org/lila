package lidraughts.importer

import draughts._
import org.specs2.mutable._
import org.specs2.matcher.ValidationMatchers

class DataFormTest extends Specification with ValidationMatchers {

  "Import from position" should {
    "preserve initial FEN" in {
      val pgn = """[Event "Casual game"]
[Result "2-0"]
[Variant "From Position"]
[Termination "Normal"]
[SetUp "1"]
[FEN "W:W31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50:B1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,19,20"]

1. 33-28 19-23 2. 28x19 13x24 { Black resigns } 2-0"""

      ImportData(pgn, None).preprocess(None) must beSuccess
    }
  }

  "Import" should {
    "default to normal termination" in {
      val pgn = """[Result "2-0"]

1. 33-28 18-23 2. 39-33 19-24 3. 28x30 20-24 4. 30x19 14x23 5. 34-29 23x34 6. 40x29 17-22 7. 31-27"""

      ImportData(pgn, None).preprocess(None) must beSuccess.like {
        case Preprocessed(g, _, _, _) => g.status must_== Status.Resign
      }
    }
  }
}

