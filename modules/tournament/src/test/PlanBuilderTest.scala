package lila.tournament

import java.time.LocalDateTime

import Schedule.Freq.*
import Schedule.Speed.*
import chess.variant.*

class PlanBuilderTest extends munit.FunSuite:

  import lila.core.i18n.*
  given Translator = TranslatorStub
  given play.api.i18n.Lang = defaultLang

  test("tourney building with stagger"):
    // Test that tourneys are scheduled based on the startAt field of the plan.
    val dt1 = LocalDateTime.of(2024, 9, 30, 12, 0)
    val plan1 = Schedule(Daily, Bullet, Standard, None, dt1).plan

    val instant2 = plan1.startsAt.plusHours(1)
    val plan2 = plan1.copy(startsAt = instant2)
    val tourney = plan2.build
    assertEquals(tourney.startsAt, instant2)
    assertEquals(tourney.schedule.get.at, dt1)

    val tourneyBsonHandler = BSONHandlers.tourHandler
    val bsonEncoded = tourneyBsonHandler.write(tourney)
    val tourney2 = tourneyBsonHandler.read(bsonEncoded)

    // Test that serialized & deserialized tourney maintains a startAt based on the plan, and
    // maintains a schedule.at based on the original schedule.
    assertEquals(tourney2.startsAt, instant2)
    assertEquals(tourney2.schedule.get.at, dt1)

  test("plansWithStagger & getNewTourneys"):
    val dt = LocalDateTime.of(2024, 9, 30, 12, 0)
    val plans = List(
      Schedule(Hourly, Bullet, Standard, None, dt).plan,
      Schedule(Hourly, Blitz, Standard, None, dt).plan,
      Schedule(Hourly, Rapid, Standard, None, dt).plan
    )
    val plannedStart = dt.instant
    // Test basic stagger function
    assertEquals(
      PlanBuilder.plansWithStagger(Nil, plans).map(_.startsAt),
      List(
        plannedStart,
        plannedStart.plusSeconds(40),
        plannedStart.plusSeconds(20)
      ),
      "plans are not staggered as expected!"
    )

    // Test that resulting tourneys are staggered in startsAt field but maintain the original schedule.at.
    val tourneys = PlanBuilder.getNewTourneys(Nil, plans)
    assertEquals(
      tourneys.map(_.startsAt),
      List(
        plannedStart,
        plannedStart.plusSeconds(40),
        plannedStart.plusSeconds(20)
      ),
      "tourneys are not staggered as expected!"
    )
    assertEquals(
      tourneys.map(_.schedule.get.at),
      List(dt, dt, dt),
      "tourneys do not maintain original schedule.at!"
    )

  test("Plan overlap"):
    val dt1 = LocalDateTime.of(2024, 9, 30, 12, 0)
    val p1 = Schedule(Daily, Bullet, Standard, None, dt1).plan
    val p2 = Schedule(Hourly, Bullet, Standard, None, dt1).plan
    val t1 = PlanBuilder.getNewTourneys(Nil, List(p1, p2))
    assertEquals(t1.length, 1, "Expected exactly one tourney!")
    assert(clue(t1.head.schedule.get).freq.isDaily)

    // Try building against with existing tourney. Nothing new should be created.
    assert(clue(PlanBuilder.getNewTourneys(t1, List(p1, p2))).isEmpty)

  test("Plan overlap, superblitz"):
    val dt1 = LocalDateTime.of(2024, 9, 30, 12, 0)
    val p1 = Schedule(Daily, SuperBlitz, Standard, None, dt1).plan
    val p2 = Schedule(Hourly, SuperBlitz, Standard, None, dt1).plan
    val t1 = PlanBuilder.getNewTourneys(Nil, List(p1, p2))
    assertEquals(t1.length, 1, "Expected exactly one tourney!")
    assert(clue(t1.head.schedule.get).freq.isDaily)

    // Try building against with existing tourney. Nothing new should be created.
    assert(clue(PlanBuilder.getNewTourneys(t1, List(p1, p2))).isEmpty)

  test("Overlap from stagger"):
    val dt1 = LocalDateTime.of(2024, 9, 30, 12, 0)
    val p1 = Schedule(Daily, Bullet, Standard, None, dt1).plan
    val p1Staggered = p1.copy(startsAt = dt1.plusMinutes(1).instant)
    val t1 = p1Staggered.build
    assertEquals(t1.startsAt, p1Staggered.startsAt)

    val dt2 = dt1.plusHours(1)
    val p2 = Schedule(Hourly, Bullet, Standard, None, dt2).plan

    // The original plan didn't conflict
    assert(!clue(p1).conflictsWith(clue(p2)))

    // But the staggered plan (from which the tourney was built) DOES conflict.
    assert(clue(p1Staggered).conflictsWith(clue(p2)))

    // Ensure that new tourney is created even if the stagger conflicts.
    PlanBuilder.getNewTourneys(List(t1), List(p2)) match
      case List(t2) if t2.scheduleFreq.has(Hourly) =>
        assert(
          clue(t2.startsAt).isBefore(clue(t1.finishesAt)),
          "Tourney schedules should conflict, but do not!"
        )
      case lst => fail(s"Unexpected tourney list: $lst")

  test("findMaxSpaceSlot"):
    def assertSlotFull(low: Long, hi: Long, existing: Seq[Long], expected: Long) =
      assertEquals(
        PlanBuilder.findMinimalGoodSlot(low, hi, existing),
        expected,
        s"low=$low hi=$hi existing=$existing"
      )

    def assertSlot(existing: Seq[Long], expected: Long) =
      assertSlotFull(0L, 100L, existing, expected)

    // Edge case: no existing slots (use low)
    assertSlot(Nil, 0L)

    // lowest maximal gap slot is returned
    assertSlot(Seq(50L), 0L)
    assertSlot(Seq(40L, 60L), 0L)

    // Middle is prioritized over high when equiv
    assertSlot(Seq(10L, 70L), 40L)

    // Middle is used if high and low are worse
    assertSlot(Seq(20L, 80L), 50L)

    // Finds slot correctly.
    assertSlot(Seq(0L), 100L)
    assertSlot(Seq(100L), 0L)
    assertSlot(Seq(40L), 100L)
    assertSlot(Seq(0L, 100L), 50L)
    assertSlot(Seq(0L, 50L, 100L), 25L)
    assertSlot(Seq(0L, 25L, 50L, 100L), 75L)
    assertSlot(Seq(0L, 25L, 50L, 75L, 100L), 12L) // Rounds down
    assertSlot(Seq(0L, 25L, 75L), 50L)

    // Edge case: low == hi
    assertSlotFull(0L, 0L, Nil, 0L)
    assertSlotFull(0L, 0L, Seq(0L), 0L)

    // Unlikely edge case: negatives
    assertSlotFull(-10L, -5L, Nil, -10L)
    assertSlotFull(-10L, -5L, Seq(-10L), -5L)
    assertSlotFull(-10L, -5L, Seq(-5L), -10L)
    assertSlotFull(-10L, -5L, Seq(-10L, -5L), -8L) // Rounds down when negative
    assertSlotFull(-1L, 2L, Seq(-1L, 2L), 0L) // Rounds down when positive
