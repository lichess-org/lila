package lila.relay

import chess.format.pgn.PgnStr
import chess.Centis
import lila.study.MultiPgn
import lila.tree.Clock

class RelayGameTest extends munit.FunSuite:

  def makeGame(pgn: String) =
    RelayFetch.multiPgnToGames.either(MultiPgn(List(PgnStr(pgn)))).getOrElse(???).head

  val g1 = makeGame:
    """
[White "Khusenkhojaev, Mustafokhuja"]
[Black "Lam, Chun Yung Samuel"]
[WhiteClock "00:33:51"]
[BlackClock "01:23:54"]
[ReferenceTime "B/2024-12-19T17:52:47.862Z"]

1. d4 Nf6
"""

  val whiteCentis = Centis.ofSeconds(33 * 60 + 51)
  val blackCentis = Centis.ofSeconds(1 * 3600 + 23 * 60 + 54)

  test("parse clock tags"):
    assertEquals(g1.tags.clocks.white, whiteCentis.some)
    assertEquals(g1.tags.clocks.black, blackCentis.some)

  test("applyTagClocksToLastMoves"):
    val applied = g1.applyTagClocksToLastMoves
    assertEquals(applied.root.lastMainlineNode.clock, Clock(blackCentis, true.some).some)
    assertEquals(applied.root.mainline.head.clock, Clock(whiteCentis, true.some).some)

  val g2 = makeGame:
    """
[WhiteClock "00:00:23"]
[BlackClock "00:00:41"]
"""

  test("parse clock tags"):
    assertEquals(g2.tags.clocks.white, Centis.ofSeconds(23).some)
    assertEquals(g2.tags.clocks.black, Centis.ofSeconds(41).some)
