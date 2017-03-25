package lila.common

import scala.annotation.tailrec

object Sequence {

  @tailrec
  final def interleave[A](base: Vector[A], a: List[A], b: List[A]): Vector[A] = a match {
    case elt :: aTail => interleave(base :+ elt, b, aTail)
    case _ => base ++ b
  }

  @tailrec
  final def interleave[A](base: Vector[A], a: Vector[A], b: Vector[A]): Vector[A] = a match {
    case elt +: aTail => interleave(base :+ elt, b, aTail)
    case _ => base ++ b
  }
}
