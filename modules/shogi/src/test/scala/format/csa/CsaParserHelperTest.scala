package shogi
package format
package csa

import format.forsyth.Sfen
import CsaParserHelper._

class CsaParserHelperTest extends ShogiTest {

  def parseAndCompare(source: String, resSfen: Sfen) =
    parseSituation(source) must beValid.like { case s =>
      s.toSfen.truncate must_== resSfen
    }

  "Handicap" in {
    parseAndCompare("PI", Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b -"))
    parseAndCompare("PI82HI22KA,-", Sfen("lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -"))
  }

  "Board" in {
    parseAndCompare(
      """P1-KY-KE+KI *  *  *  *  *  * 
P2 *  * +TO *  *  *  * +NG-KE
P3-OU-FU *  *  * +NG *  *  * 
P4 *  * -FU *  *  *  *  *  * 
P5 * -KE *  *  *  *  *  *  * 
P6 *  *  *  *  *  *  *  *  * 
P7 *  *  *  *  *  *  *  *  * 
P8 *  *  *  *  *  *  * +OU * 
P9 *  *  *  *  *  *  *  *  * 
P+00KI00GI
P-00HI00HI00KA00KA00KI00KI00GI00KE00KY00KY00KY00FU00FU00FU00FU00FU00FU00FU00FU00FU00FU00FU00FU00FU00FU00FU
-""",
      Sfen("lnG6/2+P4+Sn/kp3+S3/2p6/1n7/9/9/7K1/9 w GS2r2b2gsn3l15p")
    )
  }
}
