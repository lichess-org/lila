package lila.common.base
import org.specs2.mutable.Specification
import scala.util.Random

object LevenshteinTest {
  def check0(a: String, b: String): Boolean = {
    val d = StringUtils.levenshtein(a, b)
    !Levenshtein.isLevenshteinDistanceLessThan(a, b, d) &&
    Levenshtein.isLevenshteinDistanceLessThan(a, b, d + 1)
  }
  def check(a: String, b: String) = check0(a, b) && check0(b, a)
  def rndStr(r: Random, l: Int, sigma: Int): String = {
    val sb = new StringBuilder(l)
    for (i <- 0 until l) sb.append((48 + r.nextInt(sigma)).toChar)
    sb.result()
  }
  def rt(r: Random, l1: Int, l2: Int, sigma: Int) = {
    val s1 = rndStr(r, l1, sigma)
    val s2 = rndStr(r, l2, sigma)
    check(s1, s2)
  }
  def mt(seed: Int, nt: Int, l: Int, sigma: Int) = {
    val r = new Random(seed)
    (0 until nt).forall(i => {
      rt(r, r.nextInt(l + 1), l, sigma)
    })
  }
}

class LevenshteinTest extends Specification {
  import LevenshteinTest.{ check, mt }
  "Levenshtein" should {
    "random" in {
      mt(1, 1000, 10, 2) must beTrue
      mt(2, 1000, 10, 3) must beTrue
      mt(3, 10, 1000, 2) must beTrue
      mt(4, 10, 1000, 3) must beTrue
    }
    "empty" in {
      Levenshtein.isLevenshteinDistanceLessThan("", "", 0) must beFalse
      Levenshtein.isLevenshteinDistanceLessThan("", "", 1) must beTrue
      Levenshtein.isLevenshteinDistanceLessThan("a", "", 1) must beFalse
      Levenshtein.isLevenshteinDistanceLessThan("", "a", 1) must beFalse
      Levenshtein.isLevenshteinDistanceLessThan("a", "", 2) must beTrue
      Levenshtein.isLevenshteinDistanceLessThan("", "a", 2) must beTrue
    }
    "hand" in {
      check("aba", "a") must beTrue
      check("abb", "a") must beTrue
      check("aab", "a") must beTrue
      check("a", "abbbb") must beTrue
      check("a", "bbbba") must beTrue
      check("abacabada", "aba") must beTrue
      check("abacabada", "abacbada") must beTrue
      check("hippo", "elephant") must beTrue
      check("some", "none") must beTrue
      check("true", "false") must beTrue
      check("kitten", "mittens") must beTrue
      check("a quick brown fox jump over the lazy dog", "a slow green turtle") must beTrue
      check("I'll be back", "not today") must beTrue
      Levenshtein.isLevenshteinDistanceLessThan("cab", "abc", 3) must beTrue
      Levenshtein.isLevenshteinDistanceLessThan(
        "a quick brown fox jump over the lazy dog",
        "a slow green turtle",
        0
      ) must beFalse
      Levenshtein.isLevenshteinDistanceLessThan(
        "a quick brown fox jump over the lazy dog",
        "a slow green turtle",
        1
      ) must beFalse
    }
  }
}
