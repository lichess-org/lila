package lila.relay

import chess.format.pgn.PgnStr
import chess.TournamentClock
import chess.Clock.*

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
