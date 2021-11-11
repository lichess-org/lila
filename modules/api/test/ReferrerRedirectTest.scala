package lila.api

import org.specs2.mutable.Specification

import lila.common.config.BaseUrl

class ReferrerRedirectTest extends Specification {

  def r = new ReferrerRedirect(BaseUrl("https://lishogi.org"))

  "referrer" should {
    "be valid" in {
      r.valid("/tournament") must beTrue
      r.valid("/@/neio") must beTrue
      r.valid("/@/Neio") must beTrue
      r.valid("//lishogi.org") must beTrue
      r.valid("//foo.lichess.org") must beTrue
      r.valid("https://lishogi.org/tournament") must beTrue
      r.valid("https://lishogi.org/?a_a=b-b&C[]=#hash") must beTrue
    }
    "be invalid" in {
      r.valid("") must beFalse
      r.valid("ftp://lishogi.org/tournament") must beFalse
      r.valid("https://lichess.com") must beFalse
      r.valid("https://evil.com") must beFalse
      r.valid("https://evil.com/foo") must beFalse
      r.valid("//evil.com") must beFalse
      r.valid("//lishogi.org.evil.com") must beFalse
      r.valid("/\t/evil.com") must beFalse
      r.valid("/ /evil.com") must beFalse
    }
  }
}
