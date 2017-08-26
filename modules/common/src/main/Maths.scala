package lila.common

import scala.annotation._
import scala.util.Sorting
import scala.reflect.ClassTag

object Maths {
  def mean[T](a: Traversable[T])(implicit n: Numeric[T]): Double =
    n.toDouble(a.sum) / a.size

  def median[T: ClassTag](a: Traversable[T])(implicit n: Numeric[T]) = {
    val arr = a.toArray
    Sorting.stableSort(arr)
    val size = arr.size
    if (size == 0) Double.NaN
    else {
      val mid = size >> 1
      if (size % 2 == 0) (n.toDouble(arr(mid)) + n.toDouble(arr(mid - 1))) / 2.0
      else n.toDouble(arr(mid))
    }
  }

  def roundAt(n: Double, p: Int): BigDecimal = {
    BigDecimal(n).setScale(p, BigDecimal.RoundingMode.HALF_UP)
  }

  def toInt(l: Long): Int = l.min(Int.MaxValue).max(Int.MinValue).toInt
  def toInt(l: Option[Long]): Option[Int] = l map toInt
}
