package lila.rating

import org.apache.commons.math3.special.Erf.{ erf, erfInv }

/** Represents a Gaussian distribution over a single real variable. */
final class Gaussian(mu: Double, sigma: Double):

  def draw(): Double = mu + sigma * scalalib.ThreadLocalRandom.nextGaussian()

  /** Computes the inverse cdf of the p-value for this gaussian.
    *
    * @param p:
    *   a probability in [0,1]
    * @return
    *   x s.t. cdf(x) = numYes
    */
  def inverseCdf(p: Double): Double =
    require(p >= 0)
    require(p <= 1)

    mu + sigma * sqrt2 * erfInv(2 * p - 1)

  /** Computes the cumulative density function of the value x.
    */
  def cdf(x: Double): Double = .5 * (1 + erf((x - mu) / (sqrt2 * sigma)))

  // width in [0, 1]
  def range(center: Double, width: Double): (Double, Double) =
    val centerCdf = cdf(center)
    (
      inverseCdf(Math.max(0, centerCdf - width / 2)),
      inverseCdf(Math.min(1, centerCdf + width / 2))
    )

  private val sqrt2 = math.sqrt(2.0)
