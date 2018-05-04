package lila.security

import org.specs2.mutable.Specification

class SpamTest extends Specification {

  import Spam._

  val foobar = """foo bar"""
  val _c2 = """https://chess24.com?ref=masterpart"""

  "spam" should {
    "detect" in {
      detect(foobar) must beFalse
      detect(_c2) must beTrue
    }
    "replace" in {
      replace(foobar) must_== foobar
      replace(_c2) must_== """https://chess24.com"""
    }
  }
}
