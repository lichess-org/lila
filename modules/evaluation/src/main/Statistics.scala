package lila.evaluation

import Math.{ pow, abs, sqrt, E, exp }
import scalaz.NonEmptyList

object Statistics {
  import Erf._
  import scala.annotation._

  def variance[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = {
    val mean = average(a)
    a.map(i => pow(n.toDouble(i) - mean, 2)).list.sum / a.size
  }

  def deviation[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double =
    sqrt(variance(a))

  def average[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = {
    @tailrec def average(a: List[T], sum: T, depth: Int): Double = {
      a match {
        case Nil     => n.toDouble(sum) / depth
        case x :: xs => average(xs, n.plus(sum, x), depth + 1)
      }
    }
    average(a.tail, a.head, 1)
  }

  // Coefficient of Variance
  def coefVariation(a: NonEmptyList[Int]): Double = sqrt(variance(a)) / average(a)

  // ups all values by 5 (0.5s)
  // as to avoid very high variation on bullet games
  // where all move times are low (https://en.lichess.org/@/AlisaP?mod)
  def moveTimeCoefVariation(a: NonEmptyList[Int]): Double = coefVariation(a.map(5+))

  def moveTimeCoefVariation(pov: lila.game.Pov): Option[Double] =
    pov.game.moveTimes(pov.color).toNel.map(moveTimeCoefVariation)

  def consistentMoveTimes(pov: lila.game.Pov): Boolean =
    moveTimeCoefVariation(pov) ?? (_ < 0.4)

  def noFastMoves(pov: lila.game.Pov): Boolean = pov.game.moveTimes(pov.color).count(2>) <= 2

  def intervalToVariance4(interval: Double): Double = pow(interval / 3, 8) // roughly speaking

  // Accumulative probability function for normal distributions
  def cdf[T](x: T, avg: T, sd: T)(implicit n: Numeric[T]): Double =
    0.5 * (1 + erf(n.toDouble(n.minus(x, avg)) / (n.toDouble(sd) * sqrt(2))))

  // The probability that you are outside of abs(x-n) from the mean on both sides
  def confInterval[T](x: T, avg: T, sd: T)(implicit n: Numeric[T]): Double =
    1 - cdf(n.abs(x), avg, sd) + cdf(n.times(n.fromInt(-1), n.abs(x)), avg, sd)

  def listAverage[T](x: List[T])(implicit n: Numeric[T]): Double = x match {
    case Nil      => 0
    case a :: Nil => n.toDouble(a)
    case a :: b   => average(NonEmptyList.nel(a, b))
  }

  def listDeviation[T](x: List[T])(implicit n: Numeric[T]): Double = x match {
    case Nil      => 0
    case _ :: Nil => 0
    case a :: b   => deviation(NonEmptyList.nel(a, b))
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
