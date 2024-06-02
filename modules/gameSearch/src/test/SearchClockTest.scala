package lila.gameSearch

class SearchClockTest extends munit.FunSuite:

  test("initMin > initMax => none"):
    assertEquals(SearchClock(initMin = 10.some, initMax = 5.some), SearchClock.empty)

  test("incMin > incMax => none"):
    assertEquals(SearchClock(incMin = 10.some, incMax = 5.some), SearchClock.empty)

  test("works"):
    assertNotEquals(
      SearchClock(incMin = 1.some, incMax = 5.some, initMin = 1.some, initMax = 2.some),
      SearchClock.empty
    )
