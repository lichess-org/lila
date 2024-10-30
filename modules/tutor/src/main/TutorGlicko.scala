package lila.tutor

import lila.rating.PerfExt.toRating
import lila.rating.{ Glicko, glicko2 }

object TutorGlicko:

  private type Rating  = Int
  private type Outcome = Boolean

  private val calculator = glicko2.RatingCalculator()

  def outcomesRating(perf: Perf, outcomes: List[(Rating, Outcome)]): Rating =
    val player = perf.toRating
    val results = glicko2.BinaryRatingPeriodResults(
      outcomes.map { case (rating, outcome) =>
        glicko2.BinaryResult(player, glicko2.Rating(rating, 60, 0.06, 10), outcome)
      }
    )

    try calculator.updateRatings(results, true)
    catch case e: Exception => logger.error("TutorGlicko.outcomesRating", e)

    player.rating.toInt
