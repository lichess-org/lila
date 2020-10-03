package lila.api

import org.specs2.mutable.Specification

import lila.common.config.BaseUrl

class ReferrerRedirectTest extends Specification {

  def valid = new Util(BaseUrl("https://lichess.org")).goodReferrer _

  "referrer" should {
    "be valid" in {
      valid("/tournament") must beTrue
      valid("/@/neio") must beTrue
      valid("https://lichess.org/tournament") must beTrue
    }
    "be invalid" in {
      valid("") must beFalse
      valid("ftp://lichess.org/tournament") must beFalse
      valid("https://evil.com") must beFalse
      valid("https://evil.com/foo") must beFalse
      valid("//evil.com") must beFalse
      valid("/\t/evil.com") must beFalse
    }
  }
}
