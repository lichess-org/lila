package lila.common

import scala.Numeric.Implicits.*
import scala.reflect.ClassTag
import scala.util.Sorting
import ornicar.scalalib.ThreadLocalRandom

object Maths:

  def mean[T](a: Iterable[T])(using n: Numeric[T]): Option[Double] =
    a.nonEmpty option (n.toDouble(a.sum) / a.size)

  def median[T: ClassTag](a: Iterable[T])(using n: Numeric[T]): Option[Double] =
    a.nonEmpty.option:
      val arr = a.toArray
      Sorting.stableSort(arr)
      val size = arr.length
      val mid  = size / 2
      if size % 2 == 0
      then n.toDouble(arr(mid) + arr(mid - 1)) / 2
      else n.toDouble(arr(mid))

  def harmonicMean(a: Iterable[Double]): Option[Double] =
    a.nonEmpty.option:
      a.size / a.foldLeft(0d) { (acc, v) => acc + 1 / Math.max(1, v) }

  def weightedMean(a: Iterable[(Double, Double)]): Option[Double] =
    a.nonEmpty.so:
      a.foldLeft(0d -> 0d) { case ((av, aw), (v, w)) => (av + v * w, aw + w) } match
        case (v, w) => w != 0 option v / w

  def arithmeticAndHarmonicMean(a: Iterable[Double]): Option[Double] = for
    arithmetic <- mean(a)
    harmonic   <- harmonicMean(a)
  yield (arithmetic + harmonic) / 2

  def roundAt(n: Double, p: Int): BigDecimal =
    BigDecimal(n).setScale(p, BigDecimal.RoundingMode.HALF_UP)

  def roundDownAt(n: Double, p: Int): BigDecimal =
    BigDecimal(n).setScale(p, BigDecimal.RoundingMode.DOWN)

  def closestMultipleOf(mult: Int, v: Int): Int =
    ((2 * v + mult) / (2 * mult)) * mult

  def divideRoundUp(a: Int, b: Int): Option[Int] =
    b != 0 option Math.ceil(a.toDouble / b).toInt

  /* Moderates distribution with a factor,
   * and retries when value is outside the mean+deviation box.
   * Factor is at most 1 to prevent too many retries
   * Factor=1 => 30% retry
   * Factor=0.3 => 0.1% retry
   */
  @scala.annotation.tailrec
  def boxedNormalDistribution(mean: Int, deviation: Int, factor: Double): Int =
    val normal = mean + deviation * ThreadLocalRandom.nextGaussian() * factor.atMost(1)
    if normal > mean - deviation && normal < mean + deviation then normal.toInt
    else boxedNormalDistribution(mean, deviation, factor)

  // https://www.scribbr.com/statistics/standard-deviation/
  // using population variance
  def standardDeviation(a: Iterable[Double]): Option[Double] =
    mean(a).map: mean =>
      Math.sqrt:
        a.foldLeft(0d) { (sum, x) =>
          sum + Math.pow(x - mean, 2)
        } / a.size

  def isCloseTo[T](a: T, b: T, delta: Double)(using n: Numeric[T]) =
    (n.toDouble(a) - n.toDouble(b)).abs <= delta
