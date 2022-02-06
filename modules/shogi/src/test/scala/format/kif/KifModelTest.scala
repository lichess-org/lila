package shogi
package format
package kif

import forsyth.Sfen
import variant._
import Kif._

class KifModelTest extends ShogiTest {
  "render kif situation - board, hands, turn, from random sfen" in {
    renderSituation(
      Sfen("lnG6/2+P4+Sn/kp3+S3/2p6/1n7/9/9/7K1/9 w GS2r2b2gsn3l15p").toSituation(Standard).get
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

  "render kif situation - default minishogi" in {
    renderSetup(
      Some(Sfen("rbsgk/4p/5/P4/KGSBR b - 1")),
      Minishogi
    ) must_== """手合割：5五将棋"""
  }

  "render kif situation - minishogi" in {
    renderSituation(
      Sfen("rbsgk/4p/P4/5/KGSBR w - 2").toSituation(Minishogi).get
    ) must_== """後手の持駒：なし
  ５ ４ ３ ２ １
+---------------+
|v飛v角v銀v金v玉|一
| ・ ・ ・ ・v歩|二
| 歩 ・ ・ ・ ・|三
| ・ ・ ・ ・ ・|四
| 玉 金 銀 角 飛|五
+---------------+
先手の持駒：なし
後手番"""
  }

}
