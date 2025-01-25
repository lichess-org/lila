package lila.base

import alleycats.Zero

trait Zeros {
  implicit final val stringZero: Zero[String]         = Zero("")
  implicit final val booleanZero: Zero[Boolean]       = Zero(false)
  implicit final val intZero: Zero[Int]               = Zero(0)
  implicit final val longZero: Zero[Long]             = Zero(0L)
  implicit final val doubleZero: Zero[Double]         = Zero(0d)
  implicit final val floatZero: Zero[Float]           = Zero(0f)
  implicit final val unitZero: Zero[Unit]             = Zero(())
  @inline implicit def listZero[A]: Zero[List[A]]     = Zero(List.empty[A])
  @inline implicit def mapZero[A, B]: Zero[Map[A, B]] = Zero(Map.empty[A, B])
  @inline implicit def optionZero[A]: Zero[Option[A]] = Zero(Option.empty[A])
  @inline implicit def setZero[A]: Zero[Set[A]]       = Zero(Set.empty[A])
  @inline implicit def seqZero[A]: Zero[Seq[A]]       = Zero(Seq.empty[A])
  @inline implicit def vectorZero[A]: Zero[Vector[A]] = Zero(Vector.empty[A])
  implicit final val centisZero: Zero[shogi.Centis]   = Zero(shogi.Centis(0))
}
