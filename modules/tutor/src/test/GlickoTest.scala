package lila.tutor

class GlickoTest extends munit.FunSuite:

  test("glicko for arbitrary outcomes") {
    assertEquals(
      TutorGlicko.scoresRating(
        lila.rating.Perf.default,
        List(
          (1400, 0.8f),
          (1700, 0.6f)
        )
      ),
      1669
    )
  }
