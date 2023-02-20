package lila.api

import org.specs2.mutable.Specification

import lila.common.config.BaseUrl

class ReferrerRedirectTest extends Specification {

  def r = new ReferrerRedirect(BaseUrl("https://lishogi.org"))

  "referrer" should {
    "be valid" in {
      r.valid("/tournament") must beSome("https://lishogi.org/tournament")
      r.valid("/@/neio") must beSome("https://lishogi.org/@/neio")
      r.valid("/@/Neio") must beSome("https://lishogi.org/@/Neio")
      r.valid("//lishogi.org") must beSome("https://lishogi.org/")
      r.valid("//foo.lishogi.org") must beSome("https://foo.lishogi.org/")
      r.valid("https://lishogi.org/tournament") must beSome("https://lishogi.org/tournament")
      r.valid("https://lishogi.org/?a_a=b-b&C[]=#hash") must beSome("https://lishogi.org/?a_a=b-b&C[]=#hash")
    }
    "be invalid" in {
      r.valid("") must beNone
      r.valid("ftp://lishogi.org/tournament") must beNone
      r.valid("https://evil.com") must beNone
      r.valid("https://evil.com/foo") must beNone
      r.valid("//evil.com") must beNone
      r.valid("//lishogi.org.evil.com") must beNone
      r.valid("/\t/evil.com") must beNone
      r.valid("/ /evil.com") must beNone
      r.valid("http://lishogi.org/") must beNone // downgrade to http
    }
  }
}
