package shogi
package format
package kif
import Kif._

class KifModelTest extends ShogiTest {
  "render kif situation - board, hands, turn, from random sfen" in {
    renderSituation(
      (shogi.format.Forsyth << "lnG6/2+P4+Sn/kp3+S3/2p6/1n7/9/9/7K1/9 w GS2r2b2gsn3l15p").get
    ) must_== """後手の持駒：飛二　角二　金二　銀　桂　香三　歩十五
  ９ ８ ７ ６ ５ ４ ３ ２ １
+---------------------------+
|v香v桂 金 ・ ・ ・ ・ ・ ・|一
| ・ ・ と ・ ・ ・ ・ 全v桂|二
|v玉v歩 ・ ・ ・ 全 ・ ・ ・|三
| ・ ・v歩 ・ ・ ・ ・ ・ ・|四
| ・v桂 ・ ・ ・ ・ ・ ・ ・|五
| ・ ・ ・ ・ ・ ・ ・ ・ ・|六
| ・ ・ ・ ・ ・ ・ ・ ・ ・|七
| ・ ・ ・ ・ ・ ・ ・ 玉 ・|八
| ・ ・ ・ ・ ・ ・ ・ ・ ・|九
+---------------------------+
先手の持駒：金　銀
後手番"""
  }

}
