package lila.common

import scala.annotation.tailrec

object Sequence {

  @tailrec
  def interleave[A](base: Vector[A], a: List[A], b: List[A]): Vector[A] = a match {
    case elt :: aTail => interleave(base :+ elt, b, aTail)
    case _ => base ++ b
  }

  def interleave[A](a: Vector[A], b: Vector[A]): Vector[A] = interleave(Vector.empty, a, b)

  @tailrec
  def interleave[A](acc: Vector[A], a: Vector[A], b: Vector[A]): Vector[A] = (a, b) match {
    case (a1 +: as, b1 +: bs) => interleave(acc :+ a1 :+ b1, as, bs)
    case (Vector(), y) => acc ++ y
    case (x, Vector()) => acc ++ x
  }
}
