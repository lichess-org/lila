package lila.common

class MathsTest extends munit.FunSuite:

  import lila.common.Maths.*

  test("standard deviation empty collection") {
    assertEquals(standardDeviation(Nil), None)
  }
  test("standard deviation single value") {
    assertEquals(standardDeviation(List(3)), Some(0d))
  }
  test("standard deviation list") {
    // https://www.scribbr.com/statistics/standard-deviation/
    // ~standardDeviation(List(46, 69, 32, 60, 52, 41)) must beCloseTo(13.31, 0.01) // sample
    assert(isCloseTo(~standardDeviation(List(46, 69, 32, 60, 52, 41)), 12.15, 0.01)) // population
  }
