package lila.common

import scala.math.Ordered.orderingToOrdered

import lila.core.net.{ UserAgent, LichessMobileVersion }

class HTTPRequestTest extends munit.FunSuite:

  test("lichess mobile version from UA"):
    assertEquals(
      HTTPRequest.lichessMobileVersion(
        UserAgent("Lichess Mobile/0.16.7 as:anon sri:vRUrTavyFqpbOxMN os:Android/14 dev:SM-A155N")
      ),
      Some(LichessMobileVersion(0, 16))
    )
    assertEquals(
      HTTPRequest.lichessMobileVersion:
        UserAgent:
          "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148  Lichobile/8.0.0"
      ,
      None
    )

  test("lichess mobile version ordering"):
    assert(LichessMobileVersion(0, 16) < LichessMobileVersion(0, 17))
    assert(LichessMobileVersion(0, 9) < LichessMobileVersion(0, 10))
    assert(LichessMobileVersion(1, 1) > LichessMobileVersion(0, 10))
