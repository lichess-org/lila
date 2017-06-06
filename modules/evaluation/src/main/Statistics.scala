package lila.evaluation

import Math.{ pow, abs, sqrt, exp }
import scala.concurrent.duration._
import scalaz.{ NonEmptyList, IList }

import chess.Centis
import lila.common.Maths.{ variance, mean, deviation }

object Statistics {
  import Erf._
  import scala.annotation._

  // Coefficient of Variance
  def coefVariation(a: NonEmptyList[Int]): Double = sqrt(variance(a)) / mean(a)

  // ups all values by 0.5s
  // as to avoid very high variation on bullet games
  // where all move times are low (https://lichess.org/@/AlisaP?mod)
  def moveTimeCoefVariation(a: NonEmptyList[Centis]): Double =
    coefVariation(a.map(_.roundTenths + 5))

  def moveTimeCoefVariation(pov: lila.game.Pov): Option[Double] =
    pov.game.moveTimes(pov.color).flatMap(_.toNel.map(moveTimeCoefVariation))

  def consistentMoveTimes(pov: lila.game.Pov): Boolean =
    moveTimeCoefVariation(pov) ?? (_ < 0.4)

  private val fastMove = Centis(20)
  def noFastMoves(pov: lila.game.Pov): Boolean =
    (~pov.game.moveTimes(pov.color)).count(fastMove >) <= 2

  def intervalToVariance4(interval: Double): Double = pow(interval / 3, 8) // roughly speaking

  // Accumulative probability function for normal distributions
  def cdf[T](x: T, avg: T, sd: T)(implicit n: Numeric[T]): Double =
    0.5 * (1 + erf(n.toDouble(n.minus(x, avg)) / (n.toDouble(sd) * sqrt(2))))

  // The probability that you are outside of abs(x-n) from the mean on both sides
  def confInterval[T](x: T, avg: T, sd: T)(implicit n: Numeric[T]): Double =
    1 - cdf(n.abs(x), avg, sd) + cdf(n.times(n.fromInt(-1), n.abs(x)), avg, sd)

  def listAverage[T](x: List[T])(implicit n: Numeric[T]): Double = x match {
    case Nil => 0
    case a :: Nil => n.toDouble(a)
    case a :: b => mean(NonEmptyList.nel(a, IList fromList b))
  }

  def listDeviation[T](x: List[T])(implicit n: Numeric[T]): Double = x match {
    case Nil => 0
    case _ :: Nil => 0
    case a :: b => deviation(NonEmptyList.nel(a, IList fromList b))
  }
}

object Erf {
  // constants
  val a1: Double = 0.254829592
  val a2: Double = -0.284496736
  val a3: Double = 1.421413741
  val a4: Double = -1.453152027
  val a5: Double = 1.061405429
  val p: Double = 0.3275911

  def erf(x: Double): Double = {
    // Save the sign of x
    val sign = if (x < 0) -1 else 1
    val absx = abs(x)

    // A&S formula 7.1.26, rational approximation of error function
    val t = 1.0 / (1.0 + p * absx);
    val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-x * x);
    sign * y
  }
}
