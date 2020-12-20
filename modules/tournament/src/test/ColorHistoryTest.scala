package lila.tournament
import org.specs2.mutable.Specification

object ColorHistoryTest {
  def apply(s: String): ColorHistory = {
    s.foldLeft(ColorHistory(0, 0)) { (acc, c) =>
      c match {
        case 'W' => acc.inc(chess.White)
        case 'B' => acc.inc(chess.Black)
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
  import ColorHistoryTest.{ apply, couldPlay, firstGetsWhite, sameColors, unpack }
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
    "equals" in {
      apply("") must be equalTo apply("")
      apply("WBW") must be equalTo apply("W")
    }
  }
}
