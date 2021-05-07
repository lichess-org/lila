package lila.common

import scala.Numeric.Implicits._
import scala.reflect.ClassTag
import scala.util.Sorting

object Maths {

  def mean[T](a: Iterable[T])(implicit n: Numeric[T]): Option[Double] =
    a.nonEmpty option (n.toDouble(a.sum) / a.size)

  def median[T: ClassTag](a: Iterable[T])(implicit n: Numeric[T]) =
    a.nonEmpty option {
      val arr = a.toArray
      Sorting.stableSort(arr)
      val size = arr.length
      val mid  = size / 2
      if (size % 2 == 0) n.toDouble(arr(mid) + arr(mid - 1)) / 2
      else n.toDouble(arr(mid))
    }

  def roundAt(n: Double, p: Int): BigDecimal = {
    BigDecimal(n).setScale(p, BigDecimal.RoundingMode.HALF_UP)
  }

  def closestMultipleOf(mult: Int, v: Int): Int =
    ((2 * v + mult) / (2 * mult)) * mult

  /* Moderates distribution with a factor,
   * and retries when value is outside the mean+deviation box.
   * Factor is at most 1 to prevent too many retries
   * Factor=1 => 30% retry
   * Factor=0.3 => 0.1% retry
   */
  @scala.annotation.tailrec
  def boxedNormalDistribution(mean: Int, deviation: Int, factor: Double): Int = {
    val normal = mean + deviation * ThreadLocalRandom.nextGaussian() * factor.atMost(1)
    if (normal > mean - deviation && normal < mean + deviation) normal.toInt
    else boxedNormalDistribution(mean, deviation, factor)
  }

  def closestIn(n: Int, iter: Iterable[Int]): Option[Int] =
    iter.nonEmpty option iter.minBy(v => math.abs(v - n))
}
