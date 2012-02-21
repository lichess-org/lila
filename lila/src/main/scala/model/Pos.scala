package lila
package model

import scala.math.{ abs, min, max }

object Pos {

  val values: Set[Int] = (1 to 8) toSet

  val all: Map[String, Pos] = {
    for {
      x <- 1 to 8
      y <- 1 to 8
    } yield (xToString(x) + y.toString, Pos(x, y))
  } toMap

  def apply(s: Symbol): Pos = apply(s.name)
  def apply(k: String): Pos = apply(keyToPair(k))
  def apply(xy: Pair[Int, Int]): Pos = Pos(xy._1, xy._2)

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

case class Pos private (x: Int, y: Int) {

  if (!Pos.values(x) || !Pos.values(y))
    throw new RuntimeException("Invalid position " + (x, y))

  def ^(n: Int): Option[Pos] = if (Pos.values.contains(y + n)) Some(Pos(y + n, x)) else None
  def >(n: Int): Option[Pos] = if (Pos.values.contains(x + n)) Some(Pos(y, x + n)) else None
  def v(n: Int): Option[Pos] = this ^ (n * -1)
  def <(n: Int): Option[Pos] = this > (n * -1)
  def -?(other: Pos) = y == other.y
  def |?(other: Pos) = x == other.x
  def /?(other: Pos) = abs(x - other.x) == abs(y - other.y)
  def *?(other: Pos) = this.-?(other) || this.|?(other) || this./?(other)
  def ^^(n: Int): List[Pos] = expand(n, List(Some(this)), Pos.^).filter(_.isDefined).map(_.get).reverse
  def >>(n: Int): List[Pos] = expand(n, List(Some(this)), Pos.>).filter(_.isDefined).map(_.get).reverse
  def vv(n: Int): List[Pos] = expand(n, List(Some(this)), Pos.v).filter(_.isDefined).map(_.get).reverse
  def <<(n: Int): List[Pos] = expand(n, List(Some(this)), Pos.<).filter(_.isDefined).map(_.get).reverse
  def xToString = Pos xToString x
  def yToString  = y.toString
  override def toString = xToString + yToString
  private def expand(i: Int, accumulator: List[Option[Pos]], direct: Option[Pos] ⇒ Option[Pos]): List[Option[Pos]] = {
    if (i > 0 && accumulator.head.isDefined) expand(i - 1, direct(accumulator.head) :: accumulator, direct) else accumulator
  }
}
