package lila.rating.glicko2

// rewrite from java https://github.com/goochjs/glicko2
final class Rating(
    val rating: Double,
    val ratingDeviation: Double,
    val volatility: Double,
    val numberOfResults: Int,
    val lastRatingPeriodEnd: Option[java.time.Instant] = None
):

  import RatingCalculator.*

  // the following variables are used to hold values temporarily whilst running calculations
  private[glicko2] var workingRating: Double          = 0d
  private[glicko2] var workingRatingDeviation: Double = 0d
  private[glicko2] var workingVolatility: Double      = 0d

  /** Return the average skill value of the player scaled down to the scale used by the algorithm's internal
    * workings.
    */
  def getGlicko2Rating: Double = convertRatingToGlicko2Scale(this.rating)

  def getGlicko2RatingWithAdvantage(advantage: ColorAdvantage): Double = convertRatingToGlicko2Scale(
    this.rating + advantage.value
  )

  /** Set the average skill value, taking in a value in Glicko2 scale.
    */
  private def setGlicko2Rating(r: Double) =
    convertRatingToOriginalGlickoScale(r)

  /** Return the rating deviation of the player scaled down to the scale used by the algorithm's internal
    * workings.
    */
  def getGlicko2RatingDeviation: Double = convertRatingDeviationToGlicko2Scale(ratingDeviation)

  /** Set the rating deviation, taking in a value in Glicko2 scale.
    */
  private def setGlicko2RatingDeviation(rd: Double) =
    convertRatingDeviationToOriginalGlickoScale(rd)

  /** Used by the calculation engine, to move interim calculations into their "proper" places.
    */
  def finaliseRating() =
    Rating(
    convertRatingToOriginalGlickoScale(workingRating),
    convertRatingDeviationToOriginalGlickoScale(workingRatingDeviation),
    workingVolatility,
    numberOfResults,
    lastRatingPeriodEnd
    )


  override def toString = s"$rating / $ratingDeviation / $volatility / $numberOfResults"

  def incrementNumberOfResults(increment: Int) =
    Rating(rating, ratingDeviation, volatility, numberOfResults + increment, lastRatingPeriodEnd)
