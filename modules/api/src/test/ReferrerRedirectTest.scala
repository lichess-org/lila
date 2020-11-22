package lila.api

import org.specs2.mutable.Specification

import lila.common.config.BaseUrl

class ReferrerRedirectTest extends Specification {

  def r = new ReferrerRedirect(BaseUrl("https://lichess.org"))

  "referrer" should {
    "be valid" in {
      r.valid("/tournament") must beTrue
      r.valid("/@/neio") must beTrue
      r.valid("/@/Neio") must beTrue
      r.valid("//lichess.org") must beTrue
      r.valid("//foo.lichess.org") must beTrue
      r.valid("https://lichess.org/tournament") must beTrue
      r.valid("https://lichess.org/?a_a=b-b&C[]=#hash") must beTrue
      r.valid("https://oauth.lichess.org/oauth/authorize?response_type=code&client_id=NotReal1&redirect_uri=http%3A%2F%2Fexample.lichess.ovh%3A9371%2Foauth-callback&scope=&state=123abc") must beTrue
    }
    "be invalid" in {
      r.valid("") must beFalse
      r.valid("ftp://lichess.org/tournament") must beFalse
      r.valid("https://evil.com") must beFalse
      r.valid("https://evil.com/foo") must beFalse
      r.valid("//evil.com") must beFalse
      r.valid("//lichess.org.evil.com") must beFalse
      r.valid("/\t/evil.com") must beFalse
      r.valid("/ /evil.com") must beFalse
    }
  }
}
