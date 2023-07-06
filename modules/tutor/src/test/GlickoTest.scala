package lila.tutor

import lila.rating.Perf

class GlickoTest extends munit.FunSuite:

  test("glicko for arbitrary outcomes") {
    assertEquals(
      TutorGlicko.scoresRating(
        Perf.default,
        List(
          (1400, 0.8f),
          (1700, 0.6f)
        )
      ),
      1669
    )
  }
