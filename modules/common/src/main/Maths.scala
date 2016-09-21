package lila.common

import scala.annotation._
import scala.math.{ pow, abs, sqrt, E, exp }
import scalaz.NonEmptyList

object Maths {

  def variance[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = {
    val m = mean(a)
    a.map(i => pow(n.toDouble(i) - m, 2)).list.sum / a.size
  }

  def deviation[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = sqrt(variance(a))

  // ridiculously performance optimized mean function
  def mean[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = {
    @tailrec def recurse(a: List[T], sum: T, depth: Int): Double = {
      a match {
        case Nil     => n.toDouble(sum) / depth
        case x :: xs => recurse(xs, n.plus(sum, x), depth + 1)
      }
    }
    recurse(a.tail, a.head, 1)
  }

  def median[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = {
    val list = a.list
    val size = list.size
    val (lower, upper) = list.sorted.splitAt(size / 2)
    if (size % 2 == 0) (n.toDouble(lower.last) + n.toDouble(upper.head)) / 2.0
    else n toDouble upper.head
  }

  def truncateAt(n: Double, p: Int): Double = {
    val s = math.pow(10, p)
    (math floor n * s) / s
  }

  def toInt(l: Long): Int = l.min(Int.MaxValue).max(Int.MinValue).toInt
  def toInt(l: Option[Long]): Option[Int] = l map toInt
}
