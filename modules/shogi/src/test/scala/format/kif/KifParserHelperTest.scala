package shogi
package format
package kif

import forsyth.Sfen
import KifParserHelper._

class KifParserHelperTest extends ShogiTest {

  def parseAndCompare(source: String, handicap: Option[String], resSfen: Sfen) =
    parseSituation(source, handicap) must beValid.like { case s =>
      s.toSfen.truncate must_== resSfen
    }

  "Handicap" in {
    parseAndCompare("", Some("平手"), Sfen("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b -"))
    parseAndCompare("", Some("二枚落ち"), Sfen("lnsgkgsnl/9/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL w -"))
  }

  "BOARD" in {
    parseAndCompare(
      """後手：
      手合割：平手
      後手の持駒：金四　銀二　香三　歩十三
        ９ ８ ７ ６ ５ ４ ３ ２ １
      +---------------------------+
      | ・ ・ ・v桂 ・ ・ ・ ・ ・|一
      |v玉 角v歩 馬 ・ ・ ・ ・ ・|二
      | ・ ・ ・ ・ ・ ・ ・ ・ ・|三
      | 桂 ・ ・v歩 ・ ・ ・ ・ ・|四
      |vとv桂 ・ ・v歩 ・ ・ ・ ・|五
      | ・ ・ 飛 ・v全 ・ ・ ・ ・|六
      |v歩 桂 ・ ・ ・ ・ ・ ・ ・|七
      | ・ 香 ・ ・ ・ ・ ・ ・ ・|八
      | ・v銀 ・ ・ 龍 ・ ・ ・ ・|九
      +---------------------------+
      先手：先手
      先手の持駒：なし""",
      None,
      Sfen("3n5/kBp+B5/9/N2p5/+pn2p4/2R1+s4/pN7/1L7/1s2+R4 b 4g2s3l13p")
    )
  }
}
