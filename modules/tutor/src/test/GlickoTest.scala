package lila.tutor

import org.specs2.mutable.Specification
import org.goochjs.glicko2._

import lila.rating.{ Glicko, Perf }

class GlickoTest extends Specification {

  "glicko" should {
    "work for arbitrary outcomes" in {
      compute(
        List(
          (1400, 0.8),
          (1700, 0.6)
        )
      ) must_>= 1500
    }
  }

  private def compute(perf: Perf, results: List[(Int, Float)]): Int = {
    val VOLATILITY = Glicko.default.volatility
    val TAU        = 0.75d
    val calculator = new RatingCalculator(VOLATILITY, TAU)
    val rating     = perf.toRating
    1500
  }
}
