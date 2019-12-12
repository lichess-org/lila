package lila.common

import scala.collection.breakOut

import chess.Centis

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

  def strings(sep: String): StringIso[Strings] = Iso[String, Strings](
    str => Strings(str.split(sep).map(_.trim)(breakOut)),
    strs => strs.value mkString sep
  )

  implicit def isoIdentity[A]: Iso[A, A] = apply(identity[A] _, identity[A] _)

  implicit val stringIsoIdentity: Iso[String, String] = isoIdentity[String]

  implicit val ipAddressIso = string[IpAddress](IpAddress.apply, _.value)

  implicit val emailAddressIso = string[EmailAddress](EmailAddress.apply, _.value)

  implicit val normalizedEmailAddressIso = string[NormalizedEmailAddress](NormalizedEmailAddress.apply, _.value)

  implicit val centisIso = Iso.int[Centis](Centis.apply, _.centis)
}
