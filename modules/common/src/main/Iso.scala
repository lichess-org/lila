package lila.common

trait Iso[A, B] {
  val from: A => B
  val to: B => A
}

object Iso {

  type StringIso[B] = Iso[String, B]
  type IntIso[B] = Iso[Int, B]
  type BooleanIso[B] = Iso[Boolean, B]
  type DoubleIso[B] = Iso[Double, B]

  def apply[A, B](f: A => B, t: B => A): Iso[A, B] = new Iso[A, B] {
    val from = f
    val to = t
  }

  def string[B](from: String => B, to: B => String): StringIso[B] = apply(from, to)
  def int[B](from: Int => B, to: B => Int): IntIso[B] = apply(from, to)
  def double[B](from: Double => B, to: B => Double): DoubleIso[B] = apply(from, to)

  implicit def isoIdentity[A]: Iso[A, A] = apply(identity[A] _, identity[A] _)

  implicit val stringIsoIdentity: Iso[String, String] = isoIdentity[String]
}
