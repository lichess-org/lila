package lila.api

import lila.socket.Socket.Sri

class MobileTest extends munit.FunSuite:

  import Mobile.*

  given Conversion[String, UserAgent] = UserAgent(_)

  test("valid UAs"):
    assertEquals(
      LichessMobileUa.parse(
        "Lichess Mobile/0.2.1 (897) as:THibaULT sri:uw-y3_79sz os:android/11.0.2 dev:Moto G (4)"
      ),
      LichessMobileUa(
        "0.2.1",
        897,
        Some(UserId("thibault")),
        Sri("uw-y3_79sz"),
        "android",
        "11.0.2",
        "moto g (4)"
      ).some
    )
    assertEquals(
      LichessMobileUa.parse("Lichess Mobile/1.0.0_ALPHA-2 () as:anon sri:uwy379sz os:iOS/what-3v3r dev:"),
      LichessMobileUa("1.0.0_alpha-2", 0, None, Sri("uwy379sz"), "ios", "what-3v3r", "").some
    )

  test("old instance"):
    assertEquals(
      LichessMobileUa.parse("Lichess Mobile/1.0.0_ALPHA-2 () as:anon os:iOS/what-3v3r dev:"),
      LichessMobileUa("1.0.0_alpha-2", 0, None, Sri("old"), "ios", "what-3v3r", "").some
    )

  test("invalid UAs"):
    assertEquals(
      LichessMobileUa.parse("Mobile/0.2.1 (897) as:thibault sri:39xtnrf8 os:android/11.0.2 dev:Moto G (4)"),
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
