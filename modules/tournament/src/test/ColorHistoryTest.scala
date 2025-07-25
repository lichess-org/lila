package lila.tournament

object ColorHistoryTest:
  def apply(s: String): ColorHistory =
    s.foldLeft(ColorHistory(0, 0)) { (acc, c) =>
      c match
        case 'W' => acc.inc(chess.White)
        case 'B' => acc.inc(chess.Black)
    }
  def toTuple2(history: ColorHistory): (Int, Int) = (history.strike, history.balance)
  def unpack(s: String): (Int, Int) = toTuple2(apply(s))
  def couldPlay(s1: String, s2: String, maxStreak: Int): Boolean = apply(s1).couldPlay(apply(s2), maxStreak)
  def sameColors(s1: String, s2: String): Boolean = apply(s1).sameColors(apply(s2))
  def firstGetsWhite(s1: String, s2: String): Boolean =
    apply(s1).firstGetsWhite(apply(s2)) { () =>
      true
    }

class ColorHistoryTest extends munit.FunSuite:
  import ColorHistoryTest.{ apply, couldPlay, firstGetsWhite, sameColors, unpack }
  test("hand tests"):
    assertEquals(unpack("WWW"), ((3, 3)))
    assertEquals(unpack("WWWB"), ((-1, 2)))
    assertEquals(unpack("BBB"), ((-3, -3)))
    assertEquals(unpack("BBBW"), ((1, -2)))
    assertEquals(unpack("WWWBBB"), ((-3, 0)))
  test("couldPlay"):
    assert(!couldPlay("WWW", "WWW", 3))
    assert(!couldPlay("BBB", "BBB", 3))
    assert(couldPlay("BB", "BB", 3))
  test("sameColors"):
    assert(sameColors("WWW", "W"))
    assert(sameColors("BBB", "B"))
  test("firstGetsWhite"):
    assert(!firstGetsWhite("WWW", "WW"))
    assert(firstGetsWhite("WW", "WWW"))
    assert(firstGetsWhite("BB", "B"))
    assert(!firstGetsWhite("B", "BB"))
    assert(!firstGetsWhite("WW", "BWW"))
    assert(firstGetsWhite("BB", "WBB"))
  test("equals"):
    assertEquals(apply(""), apply(""))
    assertEquals(apply("WBW"), apply("W"))
