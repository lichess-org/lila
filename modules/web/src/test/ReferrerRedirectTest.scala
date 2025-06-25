package lila.web

import lila.core.config.BaseUrl

class ReferrerRedirectTest extends munit.FunSuite:

  def r = new ReferrerRedirect(BaseUrl("https://lichess.org"))

  test("be valid"):
    assertEquals(r.valid("/tournament"), Some("https://lichess.org/tournament"))
    assertEquals(r.valid("/@/neio"), Some("https://lichess.org/@/neio"))
    assertEquals(r.valid("/@/Neio"), Some("https://lichess.org/@/Neio"))
    assertEquals(r.valid("/"), Some("https://lichess.org/"))
    assertEquals(r.valid("https://lichess.org/tournament"), Some("https://lichess.org/tournament"))
    assertEquals(
      r.valid("https://lichess.org/?a_a=b-b&C[]=#hash"),
      Some("https://lichess.org/?a_a=b-b&C[]=#hash")
    )
    assertEquals(r.valid("/api"), Some("https://lichess.org/api"))
    assertEquals(r.valid("/something/api/something"), Some("https://lichess.org/something/api/something"))

  test("be invalid"):
    assertEquals(r.valid(""), None)
    assertEquals(r.valid("//foo.lichess.org"), None)
    assertEquals(r.valid("ftp://lichess.org/tournament"), None)
    assertEquals(r.valid("https://evil.com"), None)
    assertEquals(r.valid("https://evil.com/foo"), None)
    assertEquals(r.valid("//evil.com"), None)
    assertEquals(r.valid("//lichess.org.evil.com"), None)
    assertEquals(r.valid("/\t/evil.com"), None)
    assertEquals(r.valid("/ /evil.com"), None)
    assertEquals(r.valid("http://lichess.org/"), None) // downgrade to http
    assertEquals(r.valid("/login"), None)
    assertEquals(r.valid("/account/personal-data"), None)
    assertEquals(r.valid("/api/games/user/Cammy"), None)
    assertEquals(r.valid("/api/broadcast/abcdefgh"), None)
    assertEquals(r.valid("https://lichess.org/api/broadcast/abcdefgh"), None)
    assertEquals(r.valid("https://lichess.org/something.pgn"), None)
    assertEquals(r.valid("https://lichess.org/swiss/abcdefgh.trf"), None)
    assertEquals(r.valid("https://lichess.org/games/export/Cammy"), None)
