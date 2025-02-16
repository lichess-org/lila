package lila.relay

import chess.format.pgn.PgnStr
import chess.TournamentClock
import chess.Clock.*
import play.api.Mode
import lila.relay.RelayRoundForm.cleanUrl

class RelayFetchTest extends munit.FunSuite:

  import RelayFetch.injectTimeControl.*

  val p1 = PgnStr("""
[Event "SixDays Budapest June GMA"]
[BlackFideId "1141058"]

1. d4 { [%eval 0.16] [%clk 1:27:11] }
1... f5 { [%eval 0.5] [%clk 1:30:31] }
""")

  val p2 = PgnStr("""
[Event "SixDays Budapest June GMA"]
[TimeControl "5+3"]
[BlackFideId "1141058"]

1. d4 { [%eval 0.16] [%clk 1:27:11] }
1... f5 { [%eval 0.5] [%clk 1:30:31] }""")

  val tc = TournamentClock(LimitSeconds(15 * 60), IncrementSeconds(10))

  test("inject time control"):
    assertEquals(in(p1, none), p1)
    assertEquals(in(p2, none), p2)
    assertEquals(
      in(p1, tc.some),
      PgnStr(s"""[TimeControl "15+10"]\n${p1}""")
    )
    assertEquals(in(p2, tc.some), p2)

  given Mode                = Mode.Prod
  def parseUrl(str: String) = lila.common.url.parse(str).toOption

  test("clean source urls"):
    assertEquals(
      cleanUrl("https://example.com/games.pgn"),
      parseUrl("https://example.com/games.pgn")
    )
    assertEquals(
      cleanUrl("http://example.com/games.pgn"),
      parseUrl("http://example.com/games.pgn")
    )

  test("clean source urls: reject non-https/http schemas"):
    assertEquals(
      cleanUrl("ftp://example.com/games.pgn"),
      None
    )

  test("clean source urls: abide blacklist"):
    assertEquals(
      cleanUrl("https://google.com/games.pgn"),
      None
    )

  test("clean source urls: allow chess.com"):
    assertEquals(
      cleanUrl("https://www.chess.com/events/v1/api/1234.pgn"),
      parseUrl("https://www.chess.com/events/v1/api/1234.pgn")
    )
    assertEquals(
      cleanUrl("https://api.chess.com/pub/1234.pgn"),
      parseUrl("https://api.chess.com/pub/1234.pgn")
    )
    assertEquals(
      cleanUrl("https://chess.com/events/v1/api/1234.pgn"),
      None
    )
