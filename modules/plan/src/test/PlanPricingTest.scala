package lila.plan

class PlanPricingTest extends munit.FunSuite:

  import PlanPricingApi.nicelyRound

  test("round to nice number"):
    // val ns = {
    //   def next(i: Double, j: Double): List[Double] =
    //     // if (i > 1_000_000) List(i)
    //     if (i > 30_000) List(i)
    //     else i :: next(i + j + i / 10, j + 1)
    //   next(0, 0.01)
    // }
    // println(ns.map(n => (n, nicelyRound(n))).mkString("\n"))
    assertEquals(nicelyRound(0.3), BigDecimal(1))
    assertEquals(nicelyRound(0.9), BigDecimal(1))
    assertEquals(nicelyRound(1), BigDecimal(1))
    assertEquals(nicelyRound(1.1), BigDecimal(1))
    assertEquals(nicelyRound(1.3), BigDecimal(1))
    assertEquals(nicelyRound(1.7), BigDecimal(2))
    assertEquals(nicelyRound(3), BigDecimal(3))
    assertEquals(nicelyRound(4), BigDecimal(4))
    assertEquals(nicelyRound(6), BigDecimal(6))
    assertEquals(nicelyRound(9), BigDecimal(9))
    assertEquals(nicelyRound(11), BigDecimal(10))
    assertEquals(nicelyRound(14), BigDecimal(10))
    assertEquals(nicelyRound(15), BigDecimal(20))
    assertEquals(nicelyRound(19), BigDecimal(20))
    assertEquals(nicelyRound(23), BigDecimal(20))
    assertEquals(nicelyRound(77), BigDecimal(80))
    assertEquals(nicelyRound(1009), BigDecimal(1000))

    assertEquals(nicelyRound(99.99999999999), BigDecimal(100))
    assertEquals(nicelyRound(99_999.99999999), BigDecimal(100_000))
    assertEquals(nicelyRound(99_999.99999999999), BigDecimal(100_000))
