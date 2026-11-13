package lila.relay

import java.time.{ Duration, Instant }

import chess.Centis
import chess.format.pgn.PgnStr

import lila.study.MultiPgn
import lila.tree.Clock

class RelayGameTest extends munit.FunSuite:

  def makeGame(pgn: String) =
    RelayFetch.multiPgnToGames.parseAndCache(MultiPgn(List(PgnStr(pgn)))).getOrElse(???).head

  val g1 = makeGame:
    """
[White "Khusenkhojaev, Mustafokhuja"]
[Black "Lam, Chun Yung Samuel"]
[WhiteClock "00:33:51"]
[BlackClock "01:23:54"]
[ReferenceTime "B/2024-12-19T17:52:47.862Z"]

1. d4 Nf6
"""

  val movesHaveClocks = makeGame:
    """
[White "Khusenkhojaev, Mustafokhuja"]
[Black "Lam, Chun Yung Samuel"]
[WhiteClock "00:33:51"]
[BlackClock "01:23:54"]
[ReferenceTime "B/2024-12-19T17:52:47.862Z"]

1. d4 {[%clk 00:00:30]} Nf6 {[%clk 00:01:00]}
"""

  val whiteCentis = Centis.ofSeconds(33 * 60 + 51)
  val blackCentis = Centis.ofSeconds(1 * 3600 + 23 * 60 + 54)

  test("parse clock tags and relay reference time"):
    assertEquals(g1.tags.clocks.white, whiteCentis.some)
    assertEquals(g1.tags.clocks.black, blackCentis.some)
    assertEquals(g1.tags(_.ReferenceTime), Some("B/2024-12-19T17:52:47.862Z"))

  test("parse relay reference time prefixes and timezone offsets"):
    assertEquals(
      RelayReferenceTime.parse("B/2024-12-19T17:52:47.862Z"),
      Some(Instant.parse("2024-12-19T17:52:47.862Z"))
    )
    assertEquals(
      RelayReferenceTime.parse("W/2026-03-30T15:31:10.448+03:00"),
      Some(Instant.parse("2026-03-30T12:31:10.448Z"))
    )
    assertEquals(
      RelayReferenceTime.elapsed(
        Some("B/2026-03-30T12:30:10.448Z"),
        Some("W/2026-03-30T15:31:10.448+03:00")
      ),
      Some(Duration.ofMinutes(1))
    )

  test("subtract elapsed reference time from the active clock"):
    val adjusted = g1.subtractElapsedFromActiveClock(Duration.ofMinutes(1))
    assertEquals(
      adjusted.root.mainline.head.clock,
      Clock(whiteCentis - Centis.ofSeconds(60), true.some).some
    )
    assertEquals(adjusted.root.lastMainlineNode.clock, g1.root.lastMainlineNode.clock)

  test("applyTagClocksToLastMoves apply tag clocks when node clocks are missing"):
    val applied = g1.applyTagClocksToLastMoves
    assertEquals(applied.root.lastMainlineNode.clock, Clock(blackCentis, true.some).some)
    assertEquals(applied.root.mainline.head.clock, Clock(whiteCentis, true.some).some)

  test("applyTagClocksToLastMoves apply tag clocks and override node clocks"):
    val applied = movesHaveClocks.applyTagClocksToLastMoves
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

  val (g3, g4, g5, g6, g7, g8) = (
    makeGame("1. e4 e5"),
    makeGame("1. d4 d5"),
    makeGame("1. c4 c5"),
    makeGame("1. Nf3 Nf6"),
    makeGame("1. e4"),
    makeGame("1. d4")
  )
  val all = Vector(g1, g2, g3, g4, g5, g6, g7, g8)
  import RelayGame.Slices
  def slice(str: String) = Slices.filterAndOrder(Slices.parse(str))(all)

  test("slices filter games"):
    assertEquals(slice("1-3"), Vector(g1, g2, g3))
    assertEquals(slice("4"), Vector(g4))
    assertEquals(slice("2-3,7,8"), Vector(g2, g3, g7, g8))

  test("slices order games"):
    assertEquals(slice("7,8,2-3"), Vector(g7, g8, g2, g3))
    assertEquals(slice("3,2-4"), Vector(g3, g2, g4))
    assertEquals(slice("3,2-4,1-3"), Vector(g3, g2, g4, g1))
