package lila.tutor

import lila.rating.PerfExt.toRating
import lila.rating.{ Glicko, glicko2 }

object TutorGlicko:

  private type Rating = Int
  private type Score  = Float

  def scoresRating(perf: Perf, scores: List[(Rating, Score)]): Rating =
    val calculator = glicko2.RatingCalculator()
    val results = glicko2.RatingPeriodResults[glicko2.Result](
      perf.toRating ->
        scores.map: (rating, score) =>
          glicko2.Result(glicko2.Rating(rating, 60, 0.06, 10), score)
    )

    try calculator.computeRatings(results, true)
    catch case e: Exception => logger.error("TutorGlicko.scoresRating", e)
