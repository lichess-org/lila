package lila.relay

import java.time.Instant

import chess.{ Centis, Color }
import chess.format.UciPath
import chess.format.pgn.{ PgnStr, Tag }

import lila.core.study.data.StudyChapterName
import lila.study.{ Chapter, StudyPgnImport }
import lila.tree.Clock

class RelayClockTest extends munit.FunSuite:

  private val t0 = Instant.parse("2026-03-30T12:30:10Z")
  private val t1 = t0.plusSeconds(60)
  private val receivedAt = t1.plusSeconds(10)
  private val fourMoves = """
1. e4 {[%clk 00:58:00]} e5 {[%clk 00:57:00]}
2. Nf3 {[%clk 00:56:00]} Nc6 {[%clk 00:55:00]}
"""

  private def snapshot(
      white: Option[String] = Some("00:30:00"),
      black: Option[String] = Some("00:40:00"),
      reference: Option[String] = Some(t0.toString),
      moves: String = fourMoves,
      result: Option[String] = None
  ): RelayGame =
    val headers = List(
      Some("[White \"Alice\"]"),
      Some("[Black \"Bob\"]"),
      white.map(v => s"[WhiteClock \"$v\"]"),
      black.map(v => s"[BlackClock \"$v\"]"),
      reference.map(v => s"[ReferenceTime \"$v\"]"),
      result.map(v => s"[Result \"$v\"]")
    ).flatten.mkString("\n")
    val pgn = PgnStr(s"$headers\n\n$moves${result.fold("")(r => s" $r")}")
    StudyPgnImport.result(pgn, Nil) match
      case Right(imported) => RelayGame.fromStudyImport(imported)
      case Left(error) => fail(s"Invalid fixture PGN: $error")

  private def chapter(game: RelayGame, anchor: Option[Instant]): Chapter =
    Chapter(
      id = StudyChapterId("chapter1"),
      studyId = StudyId("study123"),
      name = StudyChapterName("Game"),
      setup = Chapter.Setup(None, game.variant, Color.White),
      root = game.root,
      tags = game.tags,
      order = 1,
      ownerId = UserId.lichess,
      relay = Some(Chapter.Relay(game.root.mainlinePath, Some(t0), None, anchor)),
      createdAt = Instant.EPOCH
    )

  private def clock(seconds: Int): Option[Clock] = Some(Clock(Centis.ofSeconds(seconds), Some(true)))

  private def clockAt(game: RelayGame, path: UciPath): Option[Clock] =
    game.root.nodeAt(path).flatMap(_.clock)

  private def update(
      chapter: Chapter,
      game: RelayGame,
      path: UciPath,
      clockChanged: Boolean,
      delaySeconds: Int = 0,
      now: Instant = receivedAt
  ): Option[Option[Instant]] =
    RelayClock.updatedClockAt(chapter, game, path, clockChanged, delaySeconds, now)

  private def applyClock(
      chapter: Chapter,
      game: RelayGame,
      path: UciPath,
      anchorUpdate: Option[Option[Instant]]
  ): Chapter =
    val rawClock = clockAt(game, path).get
    val withClock = chapter.setClock(Some(rawClock), path).get._1
    anchorUpdate.fold(withClock): anchor =>
      withClock.copy(relay = withClock.relay.map(_.copy(clockAt = anchor)))

  test("keep the active root clock when a later snapshot omits it"):
    val first = snapshot(moves = "1. e4")
    val stored = chapter(first, Some(t0))
    val missing = snapshot(black = None, moves = "1. e4")
    assert(RelayClock.preserveActiveClock(stored, missing, UciPath.root, stored.root))
    val finished = snapshot(black = None, moves = "1. e4", result = Some("1-0"))
    assert(RelayClock.preserveActiveClock(stored, finished, UciPath.root, stored.root))

  test("preserve the effective clock reference and root clock through a delayed relay"):
    val first = snapshot(moves = "1. e4")
    val source = chapter(first, Some(t0.plusSeconds(120))).updateDenorm
    val copied = RelayGame.fromChapter(source)
    assertEquals(copied.tags.clocks.black, Some(Centis.ofSeconds(40 * 60)))
    assertEquals(copied.referenceClockAt(0), Some(t0.plusSeconds(120)))
    val roundTrip = RelayGame.iso.to(RelayGame.iso.from(Vector(copied))).head
    assertEquals(roundTrip.root.clock, clock(40 * 60))
    assertEquals(roundTrip.referenceClockAt(60), Some(t0.plusSeconds(180)))

  test("preserve incoming clock tags when no clock anchor is available"):
    for reference <- List(Some(t0.toString), Some("invalid"), None) do
      val game = snapshot(reference = reference, moves = "1. e4")
      val source = chapter(game, None)
      val exportedTags = game.tags + Tag(_.Event, "Exported event")
      assertEquals(RelayGame.snapshotTags(source, exportedTags), exportedTags)
      assertEquals(RelayGame.snapshotTags(source.copy(relay = None), exportedTags), exportedTags)
      val roundTrip = RelayGame.iso.to(RelayGame.iso.from(Vector(RelayGame.fromChapter(source)))).head
      assertEquals(roundTrip.tags.clocks, game.tags.clocks)
      assertEquals(roundTrip.tags(_.ReferenceTime), game.tags(_.ReferenceTime))

    val beforeStart = snapshot(moves = "")
    assertEquals(RelayGame.snapshotTags(chapter(beforeStart, Some(t0)), beforeStart.tags), beforeStart.tags)

  test("retain raw 30:00 and 29:00 snapshots and do not re-anchor a repetition"):
    val first = snapshot()
    val next = snapshot(white = Some("00:29:00"), reference = Some(t1.toString))
    val repeated = snapshot(white = Some("00:29:00"), reference = Some(t1.toString))
    val path = first.root.mainlinePath.parent
    assertEquals(path, next.root.mainlinePath.parent)
    assertEquals(RelayClock.initialClockAt(first, first.root.mainlinePath, 0, receivedAt), Some(t0))
    val stored = chapter(first, Some(t0))
    assertEquals(
      stored.root.mainline.map(_.clock),
      List(clock(58 * 60), clock(57 * 60), clock(30 * 60), clock(40 * 60))
    )

    val anchorUpdate = update(stored, next, path, clockChanged = true)
    assertEquals(anchorUpdate, Some(Some(t1)))
    val changed = applyClock(stored, next, path, anchorUpdate).copy(tags = next.tags)
    assertEquals(
      changed.root.mainline.map(_.clock),
      List(clock(58 * 60), clock(57 * 60), clock(29 * 60), clock(40 * 60))
    )
    assertEquals(changed.relay.flatMap(_.clockAt), Some(t1))
    assertEquals(changed.relay.flatMap(_.lastMoveAt), stored.relay.flatMap(_.lastMoveAt))
    assertEquals(
      update(changed, repeated, path, clockChanged = false, now = receivedAt.plusSeconds(30)),
      None
    )
    assertEquals(clockAt(repeated, path), clock(29 * 60))

  test("a corrected clock at the same nanosecond reference keeps the BSON-normalized anchor"):
    val reference = "W/2026-03-30T15:31:10.448987654+03:00"
    val normalized = Instant.parse("2026-03-30T12:31:10.448Z")
    val first = snapshot(reference = Some(reference))
    val corrected = snapshot(
      white = Some("00:28:30"),
      reference = Some("B/2026-03-30T12:31:10.448987654Z")
    )
    val path = first.root.mainlinePath.parent
    val initial = RelayClock.initialClockAt(first, first.root.mainlinePath, 0, receivedAt)
    assertEquals(initial, Some(normalized))
    val persisted = initial.map(at => Instant.ofEpochMilli(at.toEpochMilli))
    assertEquals(persisted, initial)
    val stored = chapter(first, persisted)
    assert(!RelayClock.isOlderSnapshot(stored, corrected))
    assertEquals(update(stored, first, path, clockChanged = false), None)
    val anchorUpdate = update(stored, corrected, path, clockChanged = true)
    assertEquals(anchorUpdate, None)
    val changed = applyClock(stored, corrected, path, anchorUpdate)
    assertEquals(changed.root.nodeAt(path).flatMap(_.clock), clock(28 * 60 + 30))
    assertEquals(changed.relay.flatMap(_.clockAt), Some(normalized))
    assertEquals(update(changed, corrected, path, clockChanged = false), None)

  test("a newer reference updates the anchor even if the clock reading is identical"):
    val first = snapshot()
    val next = snapshot(reference = Some(t1.toString))
    val path = first.root.mainlinePath.parent
    val stored = chapter(first, Some(t0))
    assertEquals(clockAt(first, path), clockAt(next, path))
    val anchorUpdate = update(stored, next, path, clockChanged = false)
    assertEquals(anchorUpdate, Some(Some(t1)))
    val changed = applyClock(stored, next, path, anchorUpdate).copy(tags = next.tags)
    assertEquals(changed.root.nodeAt(path).flatMap(_.clock), clock(30 * 60))
    assertEquals(changed.root.mainline.map(_.clock), stored.root.mainline.map(_.clock))
    assertEquals(changed.relay.flatMap(_.lastMoveAt), stored.relay.flatMap(_.lastMoveAt))
    assertEquals(update(changed, next, path, clockChanged = false), None)

  test("apply the local delay to initial and updated references"):
    val first = snapshot()
    val next = snapshot(reference = Some(t1.toString))
    val path = first.root.mainlinePath.parent
    val initial = RelayClock.initialClockAt(first, first.root.mainlinePath, 120, receivedAt)
    assertEquals(initial, Some(t0.plusSeconds(120)))
    val stored = chapter(first, initial)
    val anchorUpdate = update(stored, next, path, clockChanged = false, delaySeconds = 120)
    assertEquals(anchorUpdate, Some(Some(t1.plusSeconds(120))))
    val changed = applyClock(stored, next, path, anchorUpdate)
    assertEquals(update(changed, next, path, clockChanged = false, delaySeconds = 120), None)
    assertEquals(update(stored, next, path, clockChanged = false, delaySeconds = 0), Some(Some(t1)))

  test("a turn change moves the active path without re-anchoring historical nodes"):
    val first = snapshot()
    val moves = """
1. e4 {[%clk 00:58:00]} e5 {[%clk 00:57:00]}
2. Nf3 {[%clk 00:30:00]} Nc6 {[%clk 00:40:00]}
3. Bb5 {[%clk 00:28:45]}
"""
    val next = snapshot(
      white = Some("00:28:45"),
      black = Some("00:39:00"),
      reference = Some(t1.toString),
      moves = moves
    )
    val path = next.root.mainlinePath
    val activePath = path.parent
    val historicPath = UciPath.root + next.root.mainline.head.id
    assertEquals(first.root.lastMainlineNode.ply.turn, Color.White)
    assertEquals(next.root.lastMainlineNode.ply.turn, Color.Black)
    assertEquals(activePath, first.root.mainlinePath)
    assertEquals(first.root.mainline.take(3).map(_.clock), next.root.mainline.take(3).map(_.clock))
    assertEquals(RelayClock.initialClockAt(next, path, 0, receivedAt), Some(t1))
    assertEquals(RelayClock.initialClockAt(next, activePath, 0, receivedAt), None)
    assertEquals(update(chapter(first, Some(t0)), next, activePath, clockChanged = true), None)

    val stored = chapter(next, Some(t0)).setClock(clock(40 * 60), activePath).get._1
    assertEquals(update(stored, next, UciPath.root, clockChanged = true), None)
    assertEquals(update(stored, next, historicPath, clockChanged = true), None)
    assertEquals(update(stored, next, path, clockChanged = true), None)
    val anchorUpdate = update(stored, next, activePath, clockChanged = true)
    assertEquals(anchorUpdate, Some(Some(t1)))
    val changed = applyClock(stored, next, activePath, anchorUpdate)
    assertEquals(changed.root.mainline.take(3).map(_.clock), stored.root.mainline.take(3).map(_.clock))
    assertEquals(changed.root.nodeAt(activePath).flatMap(_.clock), clock(39 * 60))
    assertEquals(changed.root.lastMainlineNode.clock, clock(28 * 60 + 45))

  test("missing or invalid references use a stable local fallback"):
    for reference <- List(None, Some("invalid"), Some("+999999999-12-31T23:59:59Z")) do
      val first = snapshot(reference = reference)
      val next = snapshot(white = Some("00:29:00"), reference = reference)
      val path = first.root.mainlinePath.parent
      val initial = RelayClock.initialClockAt(first, first.root.mainlinePath, 120, Instant.EPOCH)
      assertEquals(initial, Some(Instant.EPOCH), s"reference: $reference")
      val stored = chapter(first, initial)
      assertEquals(update(stored, first, path, clockChanged = false, delaySeconds = 120), None)
      val anchorUpdate = update(stored, next, path, clockChanged = true, delaySeconds = 120, now = t1)
      assertEquals(anchorUpdate, Some(Some(t1)), s"reference: $reference")
      val changed = applyClock(stored, next, path, anchorUpdate)
      assertEquals(update(changed, next, path, clockChanged = false, now = receivedAt.plusSeconds(30)), None)
      assertEquals(update(chapter(first, None), first, path, clockChanged = false), None)

  test("a missing or invalid active tag does not invent an anchor from historical clocks"):
    val first = snapshot()
    val stored = chapter(first, Some(t0))
    val path = first.root.mainlinePath.parent
    val sameHistoricalClock = fourMoves.replace("00:56:00", "00:30:00")
    for white <- List(None, Some("invalid")) do
      val same = snapshot(white = white, reference = Some(t1.toString), moves = sameHistoricalClock)
      val different = snapshot(white = white, reference = Some(t1.toString))
      assertEquals(same.tags.clocks.white, None)
      assertEquals(clockAt(same, path), clockAt(first, path))
      assertEquals(RelayClock.initialClockAt(same, same.root.mainlinePath, 0, receivedAt), None)
      assertEquals(update(stored, same, path, clockChanged = false), None)
      assertEquals(clockAt(different, path), clock(56 * 60))
      assertEquals(update(stored, different, path, clockChanged = true), Some(None))
      assertEquals(update(chapter(first, None), different, path, clockChanged = true), None)

    val noClocks = snapshot(white = None, black = None, moves = "1. e4 e5")
    assert(noClocks.root.mainline.forall(_.clock.isEmpty))
    assertEquals(RelayClock.initialClockAt(noClocks, noClocks.root.mainlinePath, 0, receivedAt), None)

  test("zero clock readings and epoch references are valid"):
    val zero = snapshot(white = Some("00:00:00"), reference = Some(Instant.EPOCH.toString))
    val path = zero.root.mainlinePath.parent
    assertEquals(clockAt(zero, path), clock(0))
    assertEquals(RelayClock.initialClockAt(zero, zero.root.mainlinePath, 0, receivedAt), Some(Instant.EPOCH))
    val stored = chapter(zero, None)
    val anchorUpdate = update(stored, zero, path, clockChanged = false)
    assertEquals(anchorUpdate, Some(Some(Instant.EPOCH)))
    val changed = applyClock(stored, zero, path, anchorUpdate)
    assertEquals(changed.root.nodeAt(path).flatMap(_.clock), clock(0))
    assertEquals(update(changed, zero, path, clockChanged = false), None)

  test("an older live snapshot is stale at the same path, but an older final result is not"):
    val latest = snapshot(white = Some("00:29:00"), reference = Some(t1.toString))
    val older = snapshot()
    val stored = chapter(latest, Some(t1))
    val path = latest.root.mainlinePath.parent
    assertEquals(older.root.mainlinePath, latest.root.mainlinePath)
    assert(RelayClock.isOlderSnapshot(stored, older))
    assertEquals(stored.root.nodeAt(path).flatMap(_.clock), clock(29 * 60))
    assertEquals(stored.relay.flatMap(_.clockAt), Some(t1))
    val equal = snapshot(white = Some("00:28:30"), reference = Some(t1.toString))
    assert(!RelayClock.isOlderSnapshot(stored, equal))
    val otherPath = snapshot(moves = "1. d4 d5")
    assertNotEquals(otherPath.root.mainlinePath, latest.root.mainlinePath)
    assert(!RelayClock.isOlderSnapshot(stored, otherPath))
    assert(!RelayClock.isOlderSnapshot(stored.copy(relay = None), older))
    assert(!RelayClock.isOlderSnapshot(stored.copy(tags = stored.tags - Tag.ReferenceTime), older))
    assert(
      !RelayClock.isOlderSnapshot(stored.copy(tags = stored.tags + Tag(_.ReferenceTime, "invalid")), older)
    )
    assert(!RelayClock.isOlderSnapshot(stored, snapshot(reference = Some("invalid"))))

    val finished = snapshot(
      white = Some("00:00:00"),
      black = Some("00:05:00"),
      result = Some("0-1")
    )
    assert(finished.points.isDefined)
    assert(!RelayClock.isOlderSnapshot(stored, finished))
    assertEquals(RelayClock.initialClockAt(finished, finished.root.mainlinePath, 0, receivedAt), None)
    val anchorUpdate = update(stored, finished, path, clockChanged = true)
    assertEquals(anchorUpdate, Some(None))
    assertEquals(update(stored, finished, path, clockChanged = false), Some(None))
    val withActive = applyClock(stored, finished, path, anchorUpdate)
    val ended = applyClock(withActive, finished, finished.root.mainlinePath, None)
    assertEquals(ended.root.mainline.take(2).map(_.clock), stored.root.mainline.take(2).map(_.clock))
    assertEquals(ended.root.nodeAt(path).flatMap(_.clock), clock(0))
    assertEquals(ended.root.lastMainlineNode.clock, clock(5 * 60))
    assertEquals(ended.relay.flatMap(_.clockAt), None)
    assertEquals(ended.relay.flatMap(_.lastMoveAt), stored.relay.flatMap(_.lastMoveAt))
    assertEquals(update(chapter(latest, None), finished, path, clockChanged = false), None)

  test("a finished chapter clears its anchor unless the new game explicitly resumes"):
    val first = snapshot()
    val stored = chapter(first, Some(t0)).copy(tags = first.tags + Tag(_.Result, "1-0"))
    val path = first.root.mainlinePath.parent
    val unspecified = snapshot(reference = Some(t1.toString))
    assert(unspecified.points.isEmpty)
    assertEquals(unspecified.tags(_.Result), None)
    assertEquals(update(stored, unspecified, path, clockChanged = false), Some(None))
    val resumed = snapshot(reference = Some(t1.toString), result = Some("*"))
    assert(resumed.points.isEmpty)
    assertEquals(update(stored, resumed, path, clockChanged = false), Some(Some(t1)))

  test("after one move the active clock is on the root"):
    val first = snapshot(white = Some("00:33:00"), moves = "1. e4 {[%clk 00:33:00]}")
    val next = snapshot(
      white = Some("00:33:00"),
      black = Some("00:39:00"),
      reference = Some(t1.toString),
      moves = "1. e4 {[%clk 00:33:00]}"
    )
    val path = first.root.mainlinePath
    assertEquals(path.parent, UciPath.root)
    assertEquals(first.root.lastMainlineNode.ply.turn, Color.Black)
    assertEquals(first.root.clock, clock(40 * 60))
    assertEquals(first.root.lastMainlineNode.clock, clock(33 * 60))
    val initial = RelayClock.initialClockAt(first, path, 0, receivedAt)
    assertEquals(initial, Some(t0))
    val stored = chapter(first, initial)
    val anchorUpdate = update(stored, next, UciPath.root, clockChanged = true)
    assertEquals(anchorUpdate, Some(Some(t1)))
    val changed = applyClock(stored, next, UciPath.root, anchorUpdate)
    assertEquals(changed.root.clock, clock(39 * 60))
    assertEquals(changed.root.lastMainlineNode.clock, clock(33 * 60))
    assertEquals(update(changed, next, UciPath.root, clockChanged = false), None)
    assertEquals(update(changed, next, path, clockChanged = true), None)
    val missingBlack = snapshot(black = None, moves = "1. e4")
    assertEquals(RelayClock.initialClockAt(missingBlack, path, 0, receivedAt), None)

    val finished = snapshot(
      white = Some("00:33:00"),
      black = Some("00:00:00"),
      reference = Some(t1.toString),
      moves = "1. e4",
      result = Some("1-0")
    )
    assert(finished.points.isDefined)
    val cleared = update(changed, finished, UciPath.root, clockChanged = true)
    assertEquals(cleared, Some(None))
    val ended = applyClock(changed, finished, UciPath.root, cleared)
    assertEquals(ended.root.clock, clock(0))
    assertEquals(ended.root.lastMainlineNode.clock, clock(33 * 60))
    assertEquals(ended.relay.flatMap(_.clockAt), None)

  test("no moves, missing relay, and unrelated paths cannot update an anchor"):
    val first = snapshot()
    val noMoves = snapshot(moves = "")
    assertEquals(noMoves.root.mainlinePath, UciPath.root)
    assertEquals(RelayClock.initialClockAt(noMoves, UciPath.root, 0, receivedAt), None)
    assertEquals(update(chapter(noMoves, Some(t0)), noMoves, UciPath.root, clockChanged = true), None)
    assertEquals(RelayClock.initialClockAt(first, UciPath.root, 0, receivedAt), None)
    assertEquals(
      update(chapter(first, Some(t0)).copy(relay = None), first, first.root.mainlinePath.parent, true),
      None
    )
