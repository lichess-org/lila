package lila.tutor

import org.specs2.mutable.Specification

import lila.rating.Perf

class GlickoTest extends Specification {

  "glicko" should {
    "work for arbitrary outcomes" in {
      TutorGlicko.scoresRating(
        Perf.default,
        List(
          (1400, 0.8f),
          (1700, 0.6f)
        )
      ) must_== 1669
    }
  }
}
