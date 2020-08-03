package lila.tournament
import org.specs2.mutable.Specification

object ColorHistoryTest {
  def apply(s: String): ColorHistory = {
    s.foldLeft(ColorHistory(None)) { (acc, c) =>
      c match {
        case 'W' => acc.incColor(1)
        case 'B' => acc.incColor(-1)
      }
    }
  }
  def toTuple2(history: ColorHistory): (Int, Int)                = (history.strike, history.balance)
  def unpack(s: String): (Int, Int)                              = toTuple2(apply(s))
  def couldPlay(s1: String, s2: String, maxStreak: Int): Boolean = apply(s1).couldPlay(apply(s2), maxStreak)
  def sameColors(s1: String, s2: String): Boolean                = apply(s1).sameColors(apply(s2))
  def firstGetsWhite(s1: String, s2: String): Boolean =
    apply(s1).firstGetsWhite(apply(s2)) { () =>
      true
    }
}

class ColorHistoryTest extends Specification {
  import ColorHistoryTest.{ apply, couldPlay, firstGetsWhite, sameColors, toTuple2, unpack }
  "arena tournament color history" should {
    "hand tests" in {
      unpack("WWW") must be equalTo ((3, 3))
      unpack("WWWB") must be equalTo ((-1, 2))
      unpack("BBB") must be equalTo ((-3, -3))
      unpack("BBBW") must be equalTo ((1, -2))
      unpack("WWWBBB") must be equalTo ((-3, 0))
    }
    "couldPlay" in {
      couldPlay("WWW", "WWW", 3) must beFalse
      couldPlay("BBB", "BBB", 3) must beFalse
      couldPlay("BB", "BB", 3) must beTrue
    }
    "sameColors" in {
      sameColors("WWW", "W") must beTrue
      sameColors("BBB", "B") must beTrue
    }
    "firstGetsWhite" in {
      firstGetsWhite("WWW", "WW") must beFalse
      firstGetsWhite("WW", "WWW") must beTrue
      firstGetsWhite("BB", "B") must beTrue
      firstGetsWhite("B", "BB") must beFalse
      firstGetsWhite("WW", "BWW") must beFalse
      firstGetsWhite("BB", "WBB") must beTrue
    }
    "serialization" in {
      toTuple2(ColorHistory(Some(-1))) must be equalTo ((0x7fff, 0x7fff))
      toTuple2(ColorHistory(Some(0))) must be equalTo ((-0x8000, -0x8000))
    }
    "min/(max)Value incColor" in {
      val minh = ColorHistory.minValue
      toTuple2(minh.incColor(-1)) must be equalTo toTuple2(minh)
      val maxh = ColorHistory.maxValue
      toTuple2(maxh.incColor(1)) must be equalTo toTuple2(maxh)
    }
    "equals" in {
      apply("") must be equalTo apply("")
      apply("WBW") must be equalTo apply("W")
    }
  }
}
