package lila.game

import org.specs2.mutable._

import shogi.format.forsyth.Sfen

class EngineConfigTest extends Specification {
  import EngineConfig._
  "Recognizing standard material" should {
    "should just work" in {
      // Initial
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b")
      ) must_== true
      // Missing piece
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGS1L b")
      ) must_== true
      // Promoted piece
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGS+NL b")
      ) must_== true
      // No pawns
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/9/1B5R1/LNSGKGSNL b")
      ) must_== true
      // Promoted pawns
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/p+p+p+p+p+p+p+p+p/9/9/9/+PPPPPPPPP/1B5R1/LNSGKGSNL b")
      ) must_== true
      // More pawns and tokins
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/p+p+p+p+p+p+p+p+p/9/9/9/PPPPPPPPP/1P5R1/LNSGKGSNL b")
      ) must_== false
      // More pawns and tokins
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/p+p+p+p+p+p+p+p+p/9/9/9/PPPPPPPPP/1+P5R1/LNSGKGSNL b")
      ) must_== false
      // Rook rook, bishop bishop
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5r1/ppppppppp/9/9/9/PPPPPPPPP/1B5B1/LNSGKGSNL b")
      ) must_== true
      // Three rooks
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5r1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b")
      ) must_== false
      // Hands
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/2PPPPPPP/1B5R1/LNSGKGSNL b 2p")
      ) must_== true
      // Hands
      isStandardMaterial(
        Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b p")
      ) must_== false
    }
  }
}
