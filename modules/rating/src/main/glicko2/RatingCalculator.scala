package lila.rating
package glicko2

// rewrite from java https://github.com/goochjs/glicko2
object RatingCalculator:

  private val MULTIPLIER: Double = 173.7178
  val DEFAULT_RATING: Double     = 1500.0

  def convertRatingToOriginalGlickoScale(rating: Double): Double =
    ((rating * MULTIPLIER) + DEFAULT_RATING)

  def convertRatingToGlicko2Scale(rating: Double): Double =
    ((rating - DEFAULT_RATING) / MULTIPLIER)

  def convertRatingDeviationToOriginalGlickoScale(ratingDeviation: Double): Double =
    (ratingDeviation * MULTIPLIER)

  def convertRatingDeviationToGlicko2Scale(ratingDeviation: Double): Double =
    (ratingDeviation / MULTIPLIER)

final class RatingCalculator(
    tau: Double = 0.75d,
    ratingPeriodsPerDay: Double = 0
):

  import RatingCalculator.*

  val DEFAULT_DEVIATION: Double     = 350
  val CONVERGENCE_TOLERANCE: Double = 0.000001
  val ITERATION_MAX: Int            = 1000
  val DAYS_PER_MILLI: Double        = 1.0 / (1000 * 60 * 60 * 24)

  val ratingPeriodsPerMilli: Double = ratingPeriodsPerDay * DAYS_PER_MILLI

  /** <p>Run through all players within a resultset and calculate their new ratings.</p> <p>Players within the
    * resultset who did not compete during the rating period will have see their deviation increase (in line
    * with Prof Glickman's paper).</p> <p>Note that this method will clear the results held in the association
    * resultset.</p>
    *
    * @param results
    */
  def updateRatings(results: RatingPeriodResults[?], skipDeviationIncrease: Boolean = false) =
    val players = results.getParticipants
    players.foreach { player =>
      val elapsedRatingPeriods = if skipDeviationIncrease then 0 else 1
      if results.getResults(player).sizeIs > 0 then
        calculateNewRating(player, results.getResults(player), elapsedRatingPeriods)
      else
        // if a player does not compete during the rating period, then only Step 6 applies.
        // the player's rating and volatility parameters remain the same but deviation increases
        player.workingRating = player.getGlicko2Rating
        player.workingRatingDeviation =
          calculateNewRD(player.getGlicko2RatingDeviation, player.volatility, elapsedRatingPeriods)
        player.workingVolatility = player.volatility
    }

    // now iterate through the participants and confirm their new ratings
    players.foreach { _.finaliseRating() }

  /** This is the formula defined in step 6. It is also used for players who have not competed during the
    * rating period.
    *
    * @param player
    * @param ratingPeriodEndDate
    * @param reverse
    * @return
    *   new rating deviation
    */
  def previewDeviation(player: Rating, ratingPeriodEndDate: Instant, reverse: Boolean): Double =
    var elapsedRatingPeriods = 0d
    player.lastRatingPeriodEnd.ifTrue(ratingPeriodsPerMilli > 0).foreach { periodEnd =>
      val interval = java.time.Duration.between(periodEnd, ratingPeriodEndDate)
      elapsedRatingPeriods = interval.toMillis * ratingPeriodsPerMilli
    }
    if reverse then elapsedRatingPeriods = -elapsedRatingPeriods
    val newRD = calculateNewRD(player.getGlicko2RatingDeviation, player.volatility, elapsedRatingPeriods)
    convertRatingDeviationToOriginalGlickoScale(newRD)

  /** This is the function processing described in step 5 of Glickman's paper.
    *
    * @param player
    * @param results
    * @param elapsedRatingPeriods
    */
  def calculateNewRating(player: Rating, results: List[Result], elapsedRatingPeriods: Double): Unit =
    val phi   = player.getGlicko2RatingDeviation
    val sigma = player.volatility
    val a     = Math.log(Math.pow(sigma, 2))
    val delta = deltaOf(player, results)
    val v     = vOf(player, results)

    // step 5.2 - set the initial values of the iterative algorithm to come in step 5.4
    var A: Double = a
    var B: Double = 0
    if Math.pow(delta, 2) > Math.pow(phi, 2) + v then B = Math.log(Math.pow(delta, 2) - Math.pow(phi, 2) - v)
    else
      var k = 1d
      B = a - (k * Math.abs(tau))

      while f(B, delta, phi, v, a, tau) < 0 do
        k = k + 1
        B = a - (k * Math.abs(tau))

    // step 5.3
    var fA = f(A, delta, phi, v, a, tau)
    var fB = f(B, delta, phi, v, a, tau)

    // step 5.4
    var iterations = 0
    while Math.abs(B - A) > CONVERGENCE_TOLERANCE && iterations < ITERATION_MAX do
      iterations = iterations + 1
      // println(String.format("%f - %f (%f) > %f", B, A, Math.abs(B - A), CONVERGENCE_TOLERANCE))
      val C  = A + (((A - B) * fA) / (fB - fA))
      val fC = f(C, delta, phi, v, a, tau)

      if fC * fB <= 0 then
        A = B
        fA = fB
      else fA = fA / 2.0

      B = C
      fB = fC
    if iterations == ITERATION_MAX then
      println(String.format("Convergence fail at %d iterations", iterations))
      println(player.toString())
      results.foreach(println)
      throw new RuntimeException("Convergence fail")

    val newSigma = Math.exp(A / 2.0)

    player.workingVolatility = newSigma

    // Step 6
    val phiStar = calculateNewRD(phi, newSigma, elapsedRatingPeriods)

    // Step 7
    val newPhi = 1.0 / Math.sqrt((1.0 / Math.pow(phiStar, 2)) + (1.0 / v))

    // note that the newly calculated rating values are stored in a "working" area in the Rating object
    // this avoids us attempting to calculate subsequent participants' ratings against a moving target
    player.workingRating =
      player.getGlicko2Rating + (Math.pow(newPhi, 2) * outcomeBasedRating(player, results))
    player.workingRatingDeviation = newPhi
    player.incrementNumberOfResults(results.size)

  private def f(x: Double, delta: Double, phi: Double, v: Double, a: Double, tau: Double) =
    (Math.exp(x) * (Math.pow(delta, 2) - Math.pow(phi, 2) - v - Math.exp(x)) /
      (2.0 * Math.pow(Math.pow(phi, 2) + v + Math.exp(x), 2))) -
      ((x - a) / Math.pow(tau, 2))

  /** This is the first sub-function of step 3 of Glickman's paper.
    */
  private def g(deviation: Double) =
    1.0 / (Math.sqrt(1.0 + (3.0 * Math.pow(deviation, 2) / Math.pow(Math.PI, 2))))

  /** This is the second sub-function of step 3 of Glickman's paper.
    */
  private def E(playerRating: Double, opponentRating: Double, opponentDeviation: Double) =
    1.0 / (1.0 + Math.exp(-1.0 * g(opponentDeviation) * (playerRating - opponentRating)))

  /** This is the main function in step 3 of Glickman's paper.
    */
  private def vOf(player: Rating, results: List[Result]) =
    var v = 0.0d
    for result <- results do
      v = v + ((Math.pow(g(result.getOpponent(player).getGlicko2RatingDeviation), 2))
        * E(
          player.getGlicko2Rating,
          result.getOpponent(player).getGlicko2Rating,
          result.getOpponent(player).getGlicko2RatingDeviation
        )
        * (1.0 - E(
          player.getGlicko2Rating,
          result.getOpponent(player).getGlicko2Rating,
          result.getOpponent(player).getGlicko2RatingDeviation
        )))
    1 / v

  /** This is a formula as per step 4 of Glickman's paper.
    */
  private def deltaOf(player: Rating, results: List[Result]): Double =
    vOf(player, results) * outcomeBasedRating(player, results)

  /** This is a formula as per step 4 of Glickman's paper.
    *
    * @return
    *   expected rating based on game outcomes
    */
  private def outcomeBasedRating(player: Rating, results: List[Result]) =
    var outcomeBasedRating = 0d
    for result <- results do
      outcomeBasedRating = outcomeBasedRating
        + (g(result.getOpponent(player).getGlicko2RatingDeviation)
          * (result.getScore(player) - E(
            player.getGlicko2Rating,
            result.getOpponent(player).getGlicko2Rating,
            result.getOpponent(player).getGlicko2RatingDeviation
          )))
    outcomeBasedRating

  /** This is the formula defined in step 6. It is also used for players who have not competed during the
    * rating period.
    */
  private def calculateNewRD(phi: Double, sigma: Double, elapsedRatingPeriods: Double) =
    Math.sqrt(Math.pow(phi, 2) + elapsedRatingPeriods * Math.pow(sigma, 2))
