package lila.relay

import chess.format.pgn.PgnStr
import chess.Centis
import lila.study.MultiPgn

class RelayGameTest extends munit.FunSuite:

  val pgn = """[White "Khusenkhojaev, Mustafokhuja"]
[Black "Lam, Chun Yung Samuel"]
[WhiteClock "00:33:51"]
[BlackClock "01:23:54"]
[ReferenceTime "B/2024-12-19T17:52:47.862Z"]

1. d4 Nf6
"""

  val g           = RelayFetch.multiPgnToGames(MultiPgn(List(PgnStr(pgn)))).getOrElse(???).head
  val whiteCentis = Centis.ofSeconds(33 * 60 + 51)
  val blackCentis = Centis.ofSeconds(1 * 3600 + 23 * 60 + 54)

  test("parse clock tags"):
    assertEquals(g.tags.clocks.white, whiteCentis.some)
    assertEquals(g.tags.clocks.black, blackCentis.some)
    assertEquals(g.tags(_.ReferenceTime), "B/2024-12-19T17:52:47.862Z".some)

  test("applyTagClocksToLastMoves"):
    val applied = g.applyTagClocksToLastMoves
    assertEquals(applied.root.lastMainlineNode.clock, whiteCentis.some)
    assertEquals(applied.root.mainline.head.clock, blackCentis.some)
