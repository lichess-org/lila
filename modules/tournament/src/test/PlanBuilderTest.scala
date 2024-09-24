package lila.tournament

class PlanBuilderTest extends munit.FunSuite:

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
    assertSlotFull(-1L, 2L, Seq(-1L, 2L), 0L)      // Rounds down when positive
