package lila.tournament

class PlanBuilderTest extends munit.FunSuite:

  test("findMaxSpaceSlot"):
    def assertSlotFull(low: Float, hi: Float, existing: Seq[Float], expected: Float) =
      assertEquals(
        PlanBuilder.findMinimalGoodSlot(low, hi, existing),
        expected,
        s"low=$low hi=$hi existing=$existing"
      )

    def assertSlot(existing: Seq[Float], expected: Float) =
      assertSlotFull(0f, 10f, existing, expected)

    // Edge case: no existing slots (use low)
    assertSlot(Nil, 0f)

    // Lowest maximal gap slot is returned
    assertSlot(Seq(5f), 0f)
    assertSlot(Seq(4f, 6f), 0f)

    // Middle is prioritized over high when equiv
    assertSlot(Seq(1f, 7f), 4f)

    // Middle is used if high and low are worse
    assertSlot(Seq(2f, 8f), 5f)

    // Finds slot correctly.
    assertSlot(Seq(0f), 10f)
    assertSlot(Seq(10f), 0f)
    assertSlot(Seq(4f), 10f)
    assertSlot(Seq(0f, 10f), 5f)
    assertSlot(Seq(0f, 5f, 10f), 2.5f)
    assertSlot(Seq(0f, 2.5f, 5f, 10f), 7.5f)
    assertSlot(Seq(0f, 2.5f, 5f, 7.5f, 10f), 1.25f)
    assertSlot(Seq(0f, 2.5f, 7.5f), 5f)

    // Edge case: low == hi
    assertSlotFull(0f, 0f, Nil, 0f)
    assertSlotFull(0f, 0f, Seq(0f), 0f)

    // Edge case: unrepresentable floats
    assertSlotFull(0f, .1f, Nil, 0f)
    assertSlotFull(0f, .1f, Seq(0f), .1f)
    assertSlotFull(.1f, .2f, Seq(.1f, .2f), .15f)

    // Unlikely edge case: negatives
    assertSlotFull(-10f, -5f, Nil, -10f)
    assertSlotFull(-10f, -5f, Seq(-10f), -5f)
    assertSlotFull(-10f, -5f, Seq(-5f), -10f)
    assertSlotFull(-10f, -5f, Seq(-10f, -5f), -7.5f)

    // Extremely unlikely edge case: float extremes
    import Float.{ MinValue as MinF, MaxValue as MaxF }
    assertSlotFull(MinF, 0f, Nil, MinF)
    assertSlotFull(MinF, 0f, Seq(MinF), 0f)
    assertSlotFull(MinF, 0f, Seq(0f), MinF)
    assertSlotFull(0f, MaxF, Nil, 0f)
    assertSlotFull(0f, MaxF, Seq(0f), MaxF)
    assertSlotFull(MinF, MaxF, Nil, MinF)
    assertSlotFull(MinF, MaxF, Seq(MinF), MaxF)

    // Extremely unlikely edge case: extreme ranges
    assertSlotFull(MinF, MaxF, Seq(MinF, MaxF), 0f)
