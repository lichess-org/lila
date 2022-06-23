package lila.tutor

import lila.rating.Perf
import lila.rating.Glicko
import org.goochjs.glicko2.{ FloatingRatingPeriodResults, Rating => JavaRating, RatingCalculator }

object TutorGlicko {

  private type Rating = Int
  private type Score  = Float

  private val VOLATILITY = Glicko.default.volatility
  private val TAU        = 0.75d

  def scoresRating(perf: Perf, scores: List[(Rating, Score)]): Rating = {
    val calculator = new RatingCalculator(VOLATILITY, TAU)
    val player     = perf.toRating
    val results    = new FloatingRatingPeriodResults()

    scores foreach { case (rating, score) =>
      results.addScore(player, new JavaRating(rating, 60, 0.06, 10), score)
    }

    try {
      calculator.updateRatings(results, true)
    } catch {
      case e: Exception => logger.error("TutorGlicko.scoresRating", e)
    }

    player.getRating.toInt
  }
}
