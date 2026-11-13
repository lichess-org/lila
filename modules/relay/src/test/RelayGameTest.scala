package lila.relay

import java.time.Instant

import chess.Centis
import chess.format.pgn.{ PgnStr, Tag }

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

  test("keep the latest valid reference time"):
    val previous = "B/2026-03-30T12:30:10.448Z"
    val sameInstant = "W/2026-03-30T15:30:10.448+03:00"
    val newer = "W/2026-03-30T15:31:10.448+03:00"
    assertEquals(RelayReferenceTime.latest(Some(previous), Some(newer)), Some(newer))
    assertEquals(RelayReferenceTime.latest(Some(newer), Some(previous)), Some(newer))
    assertEquals(RelayReferenceTime.latest(Some(previous), Some(sameInstant)), Some(previous))
    assertEquals(RelayReferenceTime.latest(Some(previous), Some("invalid")), Some(previous))
    assertEquals(RelayReferenceTime.latest(Some("invalid"), Some(newer)), Some(newer))
    assertEquals(RelayReferenceTime.latest(None, None), None)

  test("keep raw snapshot clocks and the same reference on repetition"):
    def snapshot(whiteClock: String, reference: String) = makeGame:
      s"""
[WhiteClock "$whiteClock"]
[BlackClock "00:40:00"]
[ReferenceTime "$reference"]

1. e4 {[%clk 00:58:00]} e5 2. Nf3 Nc6
"""
    val first = snapshot("00:30:00", "2026-03-30T12:30:10.448Z")
    val next = snapshot("00:29:00", "2026-03-30T12:31:10.448Z")
    val repeated = snapshot("00:29:00", "2026-03-30T12:31:10.448Z")
    val historical = Clock(Centis.ofSeconds(58 * 60), true.some).some
    val black = Clock(Centis.ofSeconds(40 * 60), true.some).some
    def clocks(game: RelayGame) = game.root.mainline.map(_.clock)
    assertEquals(
      clocks(first),
      List(historical, None, Clock(Centis.ofSeconds(30 * 60), true.some).some, black)
    )
    assertEquals(
      clocks(next),
      List(historical, None, Clock(Centis.ofSeconds(29 * 60), true.some).some, black)
    )
    assertEquals(clocks(repeated), clocks(next))
    assertEquals(first.referenceClockAt(0), Some(Instant.parse("2026-03-30T12:30:10.448Z")))
    assertEquals(next.referenceClockAt(0), Some(Instant.parse("2026-03-30T12:31:10.448Z")))
    assertEquals(next.referenceClockAt(120), Some(Instant.parse("2026-03-30T12:33:10.448Z")))
    assertEquals(repeated.referenceClockAt(120), next.referenceClockAt(120))

  test("shift the clock reference by the effective delay"):
    val reference = Instant.parse("2024-12-19T17:52:47.862Z")
    assertEquals(g1.referenceClockAt(0), Some(reference))
    assertEquals(g1.referenceClockAt(300), Some(reference.plusSeconds(300)))
    assertEquals(g1.referenceClockAt(-6), Some(reference.minusSeconds(6)))
    assertEquals(g1.referenceClockAt(Int.MaxValue), Some(reference.plusSeconds(Int.MaxValue.toLong)))

  test("normalize repeated reference times to BSON millisecond precision"):
    val timestamp = "W/2026-03-30T15:31:10.448987654+03:00"
    def snapshot(reference: String) = g1.copy(tags = g1.tags + Tag(_.ReferenceTime, reference))
    val first = snapshot(timestamp).referenceClockAt(120)
    val persisted = first.map(at => Instant.ofEpochMilli(at.toEpochMilli))
    val repeated = snapshot(timestamp).referenceClockAt(120)
    assertEquals(first, Some(Instant.parse("2026-03-30T12:33:10.448Z")))
    assertEquals(repeated, persisted)
    assertEquals(snapshot("2026-03-30T12:31:10.448000001Z").referenceClockAt(120), persisted)
    assertEquals(
      snapshot("1969-12-31T23:59:59.999999999Z").referenceClockAt(0),
      Some(Instant.parse("1969-12-31T23:59:59.999Z"))
    )

  test("require an explicit clock for the active side"):
    val reference = Some(Instant.parse("2024-12-19T17:52:47.862Z"))
    val whiteTurn = g1.copy(tags = g1.tags - Tag.BlackClock)
    assertEquals(whiteTurn.referenceClockAt(0), reference)
    val whiteWithoutTag = g1.copy(tags = g1.tags - Tag.WhiteClock)
    assert(whiteWithoutTag.root.mainline.head.clock.isDefined)
    assertEquals(whiteWithoutTag.referenceClockAt(0), None)
    val invalidWhite = g1.copy(tags = g1.tags + Tag(_.WhiteClock, "invalid"))
    assertEquals(invalidWhite.referenceClockAt(0), None)

    val blackTurn = makeGame:
      """
[WhiteClock "00:30:00"]
[BlackClock "00:40:00"]
[ReferenceTime "2024-12-19T17:52:47.862Z"]

1. e4
"""
    assertEquals(blackTurn.referenceClockAt(0), reference)
    val blackWithoutTag = blackTurn.copy(tags = blackTurn.tags - Tag.BlackClock)
    assert(blackWithoutTag.root.clock.isDefined)
    assertEquals(blackWithoutTag.referenceClockAt(0), None)
    val blackOnly = blackTurn.copy(tags = blackTurn.tags - Tag.WhiteClock)
    assertEquals(blackOnly.referenceClockAt(0), reference)

  test("require moves and a valid reference, while accepting future references"):
    assertEquals(g1.withoutMoves.referenceClockAt(0), None)
    assertEquals(g1.copy(tags = g1.tags - Tag.ReferenceTime).referenceClockAt(0), None)
    assertEquals(
      g1.copy(tags = g1.tags + Tag(_.ReferenceTime, "invalid")).referenceClockAt(0),
      None
    )
    val future = g1.copy(tags = g1.tags + Tag(_.ReferenceTime, "W/2999-06-01T12:00:00+03:00"))
    assertEquals(future.referenceClockAt(120), Some(Instant.parse("2999-06-01T09:02:00Z")))
    val overflowing = g1.copy(tags = g1.tags + Tag(_.ReferenceTime, "+999999999-12-31T23:59:59Z"))
    assert(RelayReferenceTime.parse("+999999999-12-31T23:59:59Z").isDefined)
    assertEquals(overflowing.referenceClockAt(Int.MaxValue), None)

  test("require the delayed reference to fit Unix milliseconds"):
    val lower = Instant.ofEpochMilli(Long.MinValue)
    val upper = Instant.ofEpochMilli(Long.MaxValue)
    def at(reference: Instant) = g1.copy(tags = g1.tags + Tag(_.ReferenceTime, reference.toString))
    assertEquals(at(lower).referenceClockAt(0), Some(lower))
    assertEquals(at(upper).referenceClockAt(0), Some(upper))
    val upperWithFraction = upper.plusNanos(999999)
    assertEquals(at(upperWithFraction).referenceClockAt(0), Some(upper))
    assertEquals(at(lower.minusNanos(1)).referenceClockAt(0), None)
    assertEquals(at(lower.minusMillis(1)).referenceClockAt(0), None)
    assertEquals(at(upper.plusMillis(1)).referenceClockAt(0), None)
    assertEquals(at(lower).referenceClockAt(-1), None)
    assertEquals(at(upper).referenceClockAt(1), None)
    assertEquals(at(lower.plusSeconds(1)).referenceClockAt(-1), Some(lower))
    assertEquals(at(upper.minusSeconds(1)).referenceClockAt(1), Some(upper))

  test("do not reference or adjust clocks of finished games"):
    val finished = makeGame:
      """
[WhiteClock "00:29:00"]
[BlackClock "00:40:00"]
[ReferenceTime "2026-03-30T12:31:10.448Z"]
[Result "1-0"]

1. d4 Nf6 1-0
"""
    assert(finished.points.isDefined)
    assertEquals(finished.referenceClockAt(300), None)
    assertEquals(finished.root.mainline.head.clock, Clock(Centis.ofSeconds(29 * 60), true.some).some)
    assertEquals(finished.root.lastMainlineNode.clock, Clock(Centis.ofSeconds(40 * 60), true.some).some)

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
