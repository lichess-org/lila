package lila.tutor

class GlickoTest extends munit.FunSuite:

  test("glicko for binary outcomes") {
    assertEquals(
      TutorGlicko.outcomesRating(
        lila.rating.Perf.default,
        List(
          (1400, true),
          (1600, false)
        )
      ),
      1500
    )
  }
