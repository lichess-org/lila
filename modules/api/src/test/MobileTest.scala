package lila.api

class MobileTest extends munit.FunSuite:

  import Mobile.*

  given Conversion[String, UserAgent] = UserAgent(_)

  test("valid UAs"):
    assertEquals(
      LichessMobileUa.parse("Lichess Mobile/0.2.1 (897) as:THibaULT os:android/11.0.2 dev:Moto G (4)"),
      LichessMobileUa("0.2.1", 897, Some(UserId("thibault")), "android", "11.0.2", "moto g (4)").some
    )
    assertEquals(
      LichessMobileUa.parse("Lichess Mobile/1.0.0_ALPHA-2 () as:anon os:iOS/what-3v3r dev:"),
      LichessMobileUa("1.0.0_alpha-2", 0, None, "ios", "what-3v3r", "").some
    )

  test("invalid UAs"):
    assertEquals(
      LichessMobileUa.parse("Mobile/0.2.1 (897) as:thibault os:android/11.0.2 dev:Moto G (4)"),
      none
    )
    assertEquals(
      LichessMobileUa.parse("Lichobile/0.2.1 (897) as:thibault os:android/11.0.2 dev:Moto G (4)"),
      none
    )
    assertEquals(
      LichessMobileUa.parse(
        "prefixed Lichess Mobile/0.2.1 (897) as:THibaULT os:android/11.0.2 dev:Moto G (4)"
      ),
      none
    )
