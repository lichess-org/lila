package lila.api

import org.specs2.mutable.Specification

import lila.common.config.BaseUrl

class ReferrerRedirectTest extends Specification {

  def r = new ReferrerRedirect(BaseUrl("https://newchess.fun"))

  "referrer" should {
    "be valid" in {
      r.valid("/tournament") must beSome("https://newchess.fun/tournament")
      r.valid("/@/neio") must beSome("https://newchess.fun/@/neio")
      r.valid("/@/Neio") must beSome("https://newchess.fun/@/Neio")
      r.valid("//newchess.fun") must beSome("https://newchess.fun/")
      r.valid("//foo.newchess.fun") must beSome("https://foo.newchess.fun/")
      r.valid("https://newchess.fun/tournament") must beSome("https://newchess.fun/tournament")
      r.valid("https://newchess.fun/?a_a=b-b&C[]=#hash") must beSome("https://newchess.fun/?a_a=b-b&C[]=#hash")
      val legacyOauth =
        "https://oauth.newchess.fun/oauth/authorize?response_type=code&client_id=NotReal1&redirect_uri=http%3A%2F%2Fexample.lichess.ovh%3A9371%2Foauth-callback&scope=challenge:read+challenge:write+board:play&state=123abc"
      r.valid(legacyOauth) must beSome(legacyOauth)
    }
    "be invalid" in {
      r.valid("") must beNone
      r.valid("ftp://newchess.fun/tournament") must beNone
      r.valid("https://evil.com") must beNone
      r.valid("https://evil.com/foo") must beNone
      r.valid("//evil.com") must beNone
      r.valid("//newchess.fun.evil.com") must beNone
      r.valid("/\t/evil.com") must beNone
      r.valid("/ /evil.com") must beNone
      r.valid("http://newchess.fun/") must beNone // downgrade to http
    }
  }
}
