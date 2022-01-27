package lila.api

import org.specs2.mutable.Specification

import lila.common.config.BaseUrl

class ReferrerRedirectTest extends Specification {

  def r = new ReferrerRedirect(BaseUrl("https://lichess.org"))

  "referrer" should {
    "be valid" in {
      r.valid("/tournament") must beSome("https://lichess.org/tournament")
      r.valid("/@/neio") must beSome("https://lichess.org/@/neio")
      r.valid("/@/Neio") must beSome("https://lichess.org/@/Neio")
      r.valid("//lichess.org") must beSome("https://lichess.org/")
      r.valid("//foo.lichess.org") must beSome("https://foo.lichess.org/")
      r.valid("https://lichess.org/tournament") must beSome("https://lichess.org/tournament")
      r.valid("https://lichess.org/?a_a=b-b&C[]=#hash") must beSome("https://lichess.org/?a_a=b-b&C[]=#hash")
      val legacyOauth =
        "https://oauth.lichess.org/oauth/authorize?response_type=code&client_id=NotReal1&redirect_uri=http%3A%2F%2Fexample.lichess.ovh%3A9371%2Foauth-callback&scope=challenge:read+challenge:write+board:play&state=123abc"
      r.valid(legacyOauth) must beSome(legacyOauth)
    }
    "be invalid" in {
      r.valid("") must beNone
      r.valid("ftp://lichess.org/tournament") must beNone
      r.valid("https://evil.com") must beNone
      r.valid("https://evil.com/foo") must beNone
      r.valid("//evil.com") must beNone
      r.valid("//lichess.org.evil.com") must beNone
      r.valid("/\t/evil.com") must beNone
      r.valid("/ /evil.com") must beNone
      r.valid("http://lichess.org/") must beNone // downgrade to http
    }
  }
}
