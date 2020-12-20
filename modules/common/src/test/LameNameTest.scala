package lila.common

import org.specs2.mutable.Specification

class LameNameTest extends Specification {

  def test = LameName.username _

  "disallow" should {
    "separated titles" in {
      test("fm-foo") must beTrue
      test("fm_foo") must beTrue
      test("wgm-foo") must beTrue
      test("wcm_foo") must beTrue
      test("gmFoobar") must beTrue
      test("gm007") must beTrue
      test("GmFoo") must beTrue
    }
    "uppercase titles" in {
      test("GMfoo") must beTrue
      test("IMfoo") must beTrue
      test("WFMfoo") must beTrue
      test("WIMfoo") must beTrue
      test("1Mfoo") must beTrue
    }
    "gross" in {
      test("Shit") must beTrue
      test("Sh1t") must beTrue
      test("douchebag") must beTrue
      test("d0uchebag") must beTrue
      test("urcunt-blah") must beTrue
      test("urcuntblah") must beTrue
      test("fuckster") must beTrue
      test("fuuckster") must beTrue
    }
  }
  "allow" should {
    "good stuff" in {
      test("joey") must beFalse
      test("gmfoo") must beFalse
      test("g-foo") must beFalse
      test("g_foo") must beFalse
      test("g-foo") must beFalse
      test("agm-foo") must beFalse
      test("atf90") must beFalse
      test("a_b") must beFalse
    }
  }
}
