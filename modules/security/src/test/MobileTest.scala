package lila.security

import lila.core.net.UserAgent
import lila.core.socket.Sri

class MobileTest extends munit.FunSuite:

  import Mobile.*

  given Conversion[String, UserAgent] = UserAgent(_)

  test("valid UAs"):
    assertEquals(
      LichessMobileUa.parse(
        "Lichess Mobile/0.2.1 (897) as:THibaULT sri:uw-y3_79sz os:Android/11.0.2 dev:Moto G (4)"
      ),
      lila.core.net
        .LichessMobileUa(
          "0.2.1",
          Some(UserId("thibault")),
          Sri("uw-y3_79sz"),
          "Android",
          "11.0.2",
          "Moto G (4)"
        )
        .some
    )
    assertEquals(
      LichessMobileUa.parse("Lichess Mobile/1.0.0_ALPHA-2 () as:anon sri:uwy379sz os:iOS/what-3v3r dev:"),
      lila.core.net.LichessMobileUa("1.0.0_ALPHA-2", None, Sri("uwy379sz"), "iOS", "what-3v3r", "").some
    )
    assertEquals(
      LichessMobileUa.parse("Lichess Mobile/1.0.0_ALPHA-2 as:anon sri:uwy379sz os:iOS/what-3v3r dev:"),
      lila.core.net.LichessMobileUa("1.0.0_ALPHA-2", None, Sri("uwy379sz"), "iOS", "what-3v3r", "").some
    )

  test("sri casing"):
    assertEquals(
      LichessMobileUa
        .parse("Lichess Mobile/1.0.0_ALPHA-2 () as:anon sri:fp_Osk6zKPF96MXI os:iOS/what-3v3r dev:")
        .map(_.sri),
      Some(Sri("fp_Osk6zKPF96MXI"))
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
        "prefixed Lichess Mobile/0.2.1 (897) as:THibaULT os:Android/11.0.2 dev:Moto G (4)"
      ),
      none
    )
