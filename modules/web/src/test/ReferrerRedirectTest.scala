package lila.web

import lila.core.config.BaseUrl

class ReferrerRedirectTest extends munit.FunSuite:

  def r = new ReferrerRedirect(BaseUrl("https://lichess.org"))

  test("be valid") {
    assertEquals(r.valid("/tournament"), Some("https://lichess.org/tournament"))
    assertEquals(r.valid("/@/neio"), Some("https://lichess.org/@/neio"))
    assertEquals(r.valid("/@/Neio"), Some("https://lichess.org/@/Neio"))
    assertEquals(r.valid("//lichess.org"), Some("https://lichess.org/"))
    assertEquals(r.valid("//foo.lichess.org"), Some("https://foo.lichess.org/"))
    assertEquals(r.valid("https://lichess.org/tournament"), Some("https://lichess.org/tournament"))
    assertEquals(
      r.valid("https://lichess.org/?a_a=b-b&C[]=#hash"),
      Some("https://lichess.org/?a_a=b-b&C[]=#hash")
    )
    val legacyOauth =
      "https://oauth.lichess.org/oauth/authorize?response_type=code&client_id=NotReal1&redirect_uri=http%3A%2F%2Fexample.lichess.ovh%3A9371%2Foauth-callback&scope=challenge:read+challenge:write+board:play&state=123abc"
    assertEquals(r.valid(legacyOauth), Some(legacyOauth))
  }
  test("be invalid") {
    assertEquals(r.valid(""), None)
    assertEquals(r.valid("ftp://lichess.org/tournament"), None)
    assertEquals(r.valid("https://evil.com"), None)
    assertEquals(r.valid("https://evil.com/foo"), None)
    assertEquals(r.valid("//evil.com"), None)
    assertEquals(r.valid("//lichess.org.evil.com"), None)
    assertEquals(r.valid("/\t/evil.com"), None)
    assertEquals(r.valid("/ /evil.com"), None)
    assertEquals(r.valid("http://lichess.org/"), None) // downgrade to http
  }
