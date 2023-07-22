package lila.security
import scala.util.Random
import lila.common.base.StringUtils
import scala.concurrent.duration.Duration

object LevenshteinTest:
  def check0(a: String, b: String): Boolean =
    val d = StringUtils.levenshtein(a, b)
    !Levenshtein.isDistanceLessThan(a, b, d) &&
    Levenshtein.isDistanceLessThan(a, b, d + 1)
  def check(a: String, b: String) = check0(a, b) && check0(b, a)
  def rndStr(r: Random, l: Int, sigma: Int): String =
    val sb = new StringBuilder(l)
    for _ <- 0 until l do sb.append((48 + r.nextInt(sigma)).toChar)
    sb.result()
  def rt(r: Random, l1: Int, l2: Int, sigma: Int) =
    val s1 = rndStr(r, l1, sigma)
    val s2 = rndStr(r, l2, sigma)
    check(s1, s2)
  def mt(seed: Int, nt: Int, l: Int, sigma: Int) =
    val r = new Random(seed)
    (0 until nt).forall(_ => rt(r, r.nextInt(l + 1), l, sigma))

class LevenshteinTest extends munit.FunSuite:

  import LevenshteinTest.{ check, mt }
  // test("Levenshtein random") {
  //   assertEquals(mt(1, 1000, 10, 2), true)
  //   assertEquals(mt(2, 1000, 10, 3), true)
  //   assertEquals(mt(3, 10, 1000, 2), true)
  //   assertEquals(mt(4, 10, 1000, 3), true)
  // }
  test("Levenshtein empty") {
    assertEquals(Levenshtein.isDistanceLessThan("", "", 0), false)
    assertEquals(Levenshtein.isDistanceLessThan("", "", 1), true)
    assertEquals(Levenshtein.isDistanceLessThan("a", "", 1), false)
    assertEquals(Levenshtein.isDistanceLessThan("", "a", 1), false)
    assertEquals(Levenshtein.isDistanceLessThan("a", "", 2), true)
    assertEquals(Levenshtein.isDistanceLessThan("", "a", 2), true)
  }
  test("Levenshtein hand") {
    assertEquals(check("aba", "a"), true)
    assertEquals(check("abb", "a"), true)
    assertEquals(check("aab", "a"), true)
    assertEquals(check("a", "abbbb"), true)
    assertEquals(check("a", "bbbba"), true)
    assertEquals(check("abacabada", "aba"), true)
    assertEquals(check("abacabada", "abacbada"), true)
    assertEquals(check("hippo", "elephant"), true)
    assertEquals(check("some", "none"), true)
    assertEquals(check("true", "false"), true)
    assertEquals(check("kitten", "mittens"), true)
    assertEquals(check("a quick brown fox jump over the lazy dog", "a slow green turtle"), true)
    assertEquals(check("I'll be back", "not today"), true)
    assertEquals(Levenshtein.isDistanceLessThan("cab", "abc", 3), true)
    assertEquals(
      Levenshtein
        .isDistanceLessThan("a quick brown fox jump over the lazy dog", "a slow green turtle", 0),
      false
    )
    assertEquals(
      Levenshtein
        .isDistanceLessThan("a quick brown fox jump over the lazy dog", "a slow green turtle", 1),
      false
    )
  }
