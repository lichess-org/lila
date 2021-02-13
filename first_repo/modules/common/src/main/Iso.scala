package lila.common

import chess.Centis
import chess.format.FEN
import play.api.i18n.Lang

trait Iso[A, B] {
  val from: A => B
  val to: B => A

  def map[BB](mapFrom: B => BB, mapTo: BB => B) = new Iso[A, BB] {
    val from = a => mapFrom(Iso.this.from(a))
    val to   = bb => Iso.this.to(mapTo(bb))
  }
}

object Iso {

  type StringIso[B]  = Iso[String, B]
  type IntIso[B]     = Iso[Int, B]
  type BooleanIso[B] = Iso[Boolean, B]
  type DoubleIso[B]  = Iso[Double, B]
  type FloatIso[B]   = Iso[Float, B]

  def apply[A, B](f: A => B, t: B => A): Iso[A, B] =
    new Iso[A, B] {
      val from = f
      val to   = t
    }

  def string[B](from: String => B, to: B => String): StringIso[B] = apply(from, to)
  def int[B](from: Int => B, to: B => Int): IntIso[B]             = apply(from, to)
  def double[B](from: Double => B, to: B => Double): DoubleIso[B] = apply(from, to)
  def float[B](from: Float => B, to: B => Float): FloatIso[B]     = apply(from, to)

  def strings(sep: String): StringIso[Strings] =
    Iso[String, Strings](
      str => Strings(str.split(sep).iterator.map(_.trim).toList),
      strs => strs.value mkString sep
    )
  def userIds(sep: String): StringIso[UserIds] =
    Iso[String, UserIds](
      str => UserIds(str.split(sep).iterator.map(_.trim.toLowerCase).toList),
      strs => strs.value mkString sep
    )
  def ints(sep: String): StringIso[Ints] =
    Iso[String, Ints](
      str => Ints(str.split(sep).iterator.map(_.trim).flatMap(_.toIntOption).toList),
      strs => strs.value mkString sep
    )

  implicit def isoIdentity[A]: Iso[A, A] = apply(identity[A], identity[A])

  implicit val stringIsoIdentity: Iso[String, String] = isoIdentity[String]

  implicit val ipAddressIso = string[IpAddress](IpAddress.apply, _.value)

  implicit val emailAddressIso = string[EmailAddress](EmailAddress.apply, _.value)

  implicit val normalizedEmailAddressIso =
    string[NormalizedEmailAddress](NormalizedEmailAddress.apply, _.value)

  implicit val centisIso = int[Centis](Centis.apply, _.centis)

  implicit val langIso = string[Lang](Lang.apply, _.toString)

  implicit val fenIso = string[FEN](FEN.apply, _.value)
}
