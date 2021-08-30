package shogi
package format.pgn
import KifUtils._


class KifUtilTest extends ShogiTest {

  "kanji to int" in {
    kanjiToInt("一") must_== 1
    kanjiToInt("十") must_== 10
    kanjiToInt("十八") must_== 18
    kanjiToInt("二十") must_== 20
    kanjiToInt("三十一") must_== 31
  }

  "kanji to int and back" in {
      (1 to 999).forall { i =>
          kanjiToInt(intToKanji(i)) must_== i
      }
  }

  "write kif situation - board, hands, turn, from random sfen" in {
      writeKifSituation(
        shogi.format.FEN("lnG6/2+P4+Sn/kp3+S3/2p6/1n7/9/9/7K1/9 w GS2r2b2gsn3l15p")
      ) must_== """後手の持駒：飛二 角二 金二 銀 桂 香三 歩十五
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
先手の持駒：金 銀
後手番"""
  }

}

