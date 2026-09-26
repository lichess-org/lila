package lila.web

import lila.core.config.BaseUrl

class ReferrerRedirectTest extends munit.FunSuite:

  def r = new ReferrerRedirect(BaseUrl("https://lichess.org"))
  def valid(ref: String) = r.valid(ref).map(_.value)

  test("be valid"):
    assertEquals(valid("/tournament"), Some("https://lichess.org/tournament"))
    assertEquals(valid("/@/neio"), Some("https://lichess.org/@/neio"))
    assertEquals(valid("/@/Neio"), Some("https://lichess.org/@/Neio"))
    assertEquals(valid("/"), Some("https://lichess.org/"))
    assertEquals(valid("https://lichess.org/tournament"), Some("https://lichess.org/tournament"))
    assertEquals(
      valid("https://lichess.org/?a_a=b-b&C[]=#hash"),
      Some("https://lichess.org/?a_a=b-b&C[]=#hash")
    )
    assertEquals(valid("/api"), Some("https://lichess.org/api"))
    assertEquals(valid("/something/api/something"), Some("https://lichess.org/something/api/something"))

  test("be invalid"):
    assertEquals(valid(""), None)
    assertEquals(valid("//foo.lichess.org"), None)
    assertEquals(valid("ftp://lichess.org/tournament"), None)
    assertEquals(valid("https://evil.com"), None)
    assertEquals(valid("https://evil.com/foo"), None)
    assertEquals(valid("//evil.com"), None)
    assertEquals(valid("//lichess.org.evil.com"), None)
    assertEquals(valid("/\t/evil.com"), None)
    assertEquals(valid("/ /evil.com"), None)
    assertEquals(valid("http://lichess.org/"), None) // downgrade to http
    assertEquals(valid("/login"), None)
    assertEquals(valid("/account/personal-data"), None)
    assertEquals(valid("/api/games/user/Cammy"), None)
    assertEquals(valid("/api/broadcast/abcdefgh"), None)
    assertEquals(valid("https://lichess.org/api/broadcast/abcdefgh"), None)
    assertEquals(valid("https://lichess.org/something.pgn"), None)
    assertEquals(valid("https://lichess.org/swiss/abcdefgh.trf"), None)
    assertEquals(valid("https://lichess.org/games/export/Cammy"), None)
