package lila.security

import org.specs2.mutable.Specification

import lila.common.Strings

class SpamTest extends Specification {

  val spam   = new Spam(() => Strings(Nil))
  val foobar = """foo bar"""
  val _c2    = """https://chess24.com?ref=masterpart"""

  "spam" >> {
    "detect" >> {
      spam.detect(foobar) must beFalse
      spam.detect(_c2) must beTrue
    }
    "replace" >> {
      spam.replace(foobar) === foobar
      spam.replace(_c2) === """https://chess24.com"""
    }
  }
}
