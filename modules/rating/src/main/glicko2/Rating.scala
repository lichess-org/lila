package lila.rating.glicko2

// rewrite from java https://github.com/goochjs/glicko2
final class Rating(
    var rating: Double,
    var ratingDeviation: Double,
    var volatility: Double,
    var numberOfResults: Int,
    var lastRatingPeriodEnd: Option[java.time.Instant] = None
):

  import RatingCalculator.*

  // the following variables are used to hold values temporarily whilst running calculations
  private[glicko2] var workingRating: Double          = scala.compiletime.uninitialized
  private[glicko2] var workingRatingDeviation: Double = scala.compiletime.uninitialized
  private[glicko2] var workingVolatility: Double      = scala.compiletime.uninitialized

  /** Return the average skill value of the player scaled down to the scale used by the algorithm's internal
    * workings.
    */
  def getGlicko2Rating: Double = convertRatingToGlicko2Scale(this.rating)

  /** Set the average skill value, taking in a value in Glicko2 scale.
    */
  def setGlicko2Rating(r: Double) =
    rating = convertRatingToOriginalGlickoScale(r)

  /** Return the rating deviation of the player scaled down to the scale used by the algorithm's internal
    * workings.
    */
  def getGlicko2RatingDeviation: Double = convertRatingDeviationToGlicko2Scale(ratingDeviation)

  /** Set the rating deviation, taking in a value in Glicko2 scale.
    */
  def setGlicko2RatingDeviation(rd: Double) =
    ratingDeviation = convertRatingDeviationToOriginalGlickoScale(rd)

  /** Used by the calculation engine, to move interim calculations into their "proper" places.
    */
  def finaliseRating() =
    setGlicko2Rating(workingRating)
    setGlicko2RatingDeviation(workingRatingDeviation)
    volatility = workingVolatility
    workingRatingDeviation = 0d
    workingRating = 0d
    workingVolatility = 0d

  override def toString = s"$rating / $ratingDeviation / $volatility / $numberOfResults"

  def incrementNumberOfResults(increment: Int) =
    numberOfResults = numberOfResults + increment
