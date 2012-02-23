package lila
package model

import scala.math.{ abs, min, max }

object Pos {

  private val values: Set[Int] = (1 to 8) toSet

  private val allKeys: Map[String, Pos] = {
    for {
      x <- 1 to 8
      y <- 1 to 8
    } yield (xToString(x) + y.toString, Pos(x, y))
  } toMap

  implicit def apply(s: Symbol): Pos = apply(s.name)
  implicit def apply(k: String): Pos = allKeys(k)

  def ^(p: Option[Pos]): Option[Pos] = p.flatMap(_ ^ 1)
  def >(p: Option[Pos]): Option[Pos] = p.flatMap(_ > 1)
  def v(p: Option[Pos]): Option[Pos] = p.flatMap(_ v 1)
  def <(p: Option[Pos]): Option[Pos] = p.flatMap(_ < 1)

  def noop(p: Option[Pos]) = p

  def vectorBasedPoss(from: Pos, directions: List[List[Option[Pos] ⇒ Option[Pos]]]) = {
    def expand(direction: Seq[Option[Pos] ⇒ Option[Pos]]): List[Pos] = {
      def next(acc: List[Pos]): List[Pos] = {
        val seed: Option[Pos] = Some(acc.headOption.getOrElse(from))
        val candidate = direction.foldLeft(seed) { (intermediate, step) ⇒ step(intermediate) }
        candidate match {
          case Some(p) ⇒ next(p :: acc)
          case None    ⇒ acc
        }
      }
      next(Nil).reverse
    }
    directions.foldLeft(Nil: List[List[Pos]]) { (acc, next) ⇒ expand(next) :: acc }.filter(l ⇒ !l.isEmpty)
  }

  def radialBasedPoss(from: Pos, offsets: Iterable[Int], filter: (Int, Int) ⇒ Boolean) = {
    (for (y ← offsets; x ← offsets; if (filter(y, x))) yield (from ^ y).flatMap(_ < x)).filter(_.isDefined).map(_.get)
  }

  private def keyToPair(k: String): Pair[Int, Int] =
    (k.charAt(0).asDigit - 9, k.charAt(1).asDigit)

  def xToString(x: Int) = (96 + x).toChar.toString
}

case class Pos(x: Int, y: Int) {

  import Pos.values

  if (!values(x) || !values(y))
    throw new RuntimeException("Invalid position " + (x, y))

  def ^(n: Int): Option[Pos] = if (values(y + n)) Some(Pos(y + n, x)) else None
  def >(n: Int): Option[Pos] = if (values(x + n)) Some(Pos(y, x + n)) else None
  def v(n: Int): Option[Pos] = this ^ (n * -1)
  def <(n: Int): Option[Pos] = this > (n * -1)
  def -?(other: Pos) = y == other.y
  def |?(other: Pos) = x == other.x
  def /?(other: Pos) = abs(x - other.x) == abs(y - other.y)
  def *?(other: Pos) = (this -? other) || (this |? other) || (this /? other)
  def ^^(n: Int): List[Pos] = <>(n, Pos.^)
  def >>(n: Int): List[Pos] = <>(n, Pos.>)
  def vv(n: Int): List[Pos] = <>(n, Pos.v)
  def <<(n: Int): List[Pos] = <>(n, Pos.<)
  def <>(n: Int, d: Option[Pos] => Option[Pos]) =
    expand(n, List(Some(this)), d).flatten reverse

  def xToString = Pos xToString x
  def yToString  = y.toString

  override def toString = xToString + yToString

  private def expand(i: Int, accumulator: List[Option[Pos]], direct: Option[Pos] ⇒ Option[Pos]): List[Option[Pos]] = {
    if (i > 0 && accumulator.head.isDefined)
      expand(i - 1, direct(accumulator.head) :: accumulator, direct)
    else accumulator
  }
}
