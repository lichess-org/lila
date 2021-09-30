package shogi
package format
package kif
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

}
