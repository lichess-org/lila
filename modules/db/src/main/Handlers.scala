package lila.db

import org.joda.time.DateTime
import reactivemongo.api.bson._
import scalaz.NonEmptyList

import lila.common.Iso._
import lila.common.{ Iso, IpAddress, EmailAddress, NormalizedEmailAddress }

trait Handlers {

  implicit val BSONJodaDateTimeHandler = new BSONHandler[DateTime] {
    def read(x: BSONDateTime) = new DateTime(x.value)
    def write(x: DateTime) = BSONDateTime(x.getMillis)
  }

  def isoHandler[A, B](iso: Iso[B, A])(implicit handler: BSONHandler[B]): BSONHandler[A] = new BSONHandler[A] {
    def read(x: BSONValue): A = iso.from(handler.readTry(x).get)
    def write(x: A): BSONValue = handler.writeTry(iso.to(x)).get
  }
  def isoHandler[A, B](to: A => B, from: B => A)(implicit handler: BSONHandler[B]): BSONHandler[A] =
    isoHandler(Iso(from, to))

  def stringIsoHandler[A](implicit iso: StringIso[A]): BSONHandler[A] = isoHandler[A, String](iso)
  def stringAnyValHandler[A](to: A => String, from: String => A): BSONHandler[A] = stringIsoHandler(Iso(from, to))

  def intIsoHandler[A](implicit iso: IntIso[A]): BSONHandler[A] = isoHandler[A, Int](iso)
  def intAnyValHandler[A](to: A => Int, from: Int => A): BSONHandler[A] = intIsoHandler(Iso(from, to))

  def booleanIsoHandler[A](implicit iso: BooleanIso[A]): BSONHandler[A] = isoHandler[A, Boolean](iso)
  def booleanAnyValHandler[A](to: A => Boolean, from: Boolean => A): BSONHandler[A] = booleanIsoHandler(Iso(from, to))

  def doubleIsoHandler[A](implicit iso: DoubleIso[A]): BSONHandler[A] = isoHandler[A, Double](iso)
  def doubleAnyValHandler[A](to: A => Double, from: Double => A): BSONHandler[A] = doubleIsoHandler(Iso(from, to))

  def dateIsoHandler[A](implicit iso: Iso[DateTime, A]): BSONHandler[A] = isoHandler[A, DateTime](iso)

  implicit def nullableHandler[T](implicit reader: BSONReader[T], writer: BSONWriter[T]): BSONHandler[Option[T]] = new BSONHandler[Option[T]] {
    private val generalizedReader = reader.asInstanceOf[BSONReader[T]]
    def read(bv: BSONValue): Option[T] = generalizedReader.readOpt(bv)
    def write(v: Option[T]): BSONValue = v.fold[BSONValue](BSONNull)(v => writer.writeTry(v).get)
  }

  implicit def bsonArrayToListHandler[T](implicit handler: BSONHandler[T]): BSONHandler[List[T]] = new BSONHandler[List[T]] {
    def read(array: BSONArray) = array.values.view.flatMap(handler.readOpt).to(List)
    def write(repr: List[T]) = BSONArray(repr.flatMap(handler.writeOpt))
  }

  implicit def bsonArrayToVectorHandler[T](implicit handler: BSONHandler[T]): BSONHandler[Vector[T]] = new BSONHandler[Vector[T]] {
    def read(array: BSONArray) = array.values.view.flatMap(handler.readOpt).to(Vector)
    def write(repr: Vector[T]) = BSONArray(repr.flatMap(handler.writeOpt))
  }

  implicit def bsonArrayToNonEmptyListHandler[T](implicit handler: BSONHandler[T]): BSONHandler[NonEmptyList[T]] = new BSONHandler[NonEmptyList[T]] {
    private val listHandler = bsonArrayToListHandler[T]
    def read(array: BSONArray) = listHandler.readOpt(array).flatMap(_.toNel) err s"BSONArray is empty, can't build NonEmptyList"
    def write(repr: NonEmptyList[T]) = listHandler.writeTry(repr.toList).get
  }

  implicit val ipAddressHandler = isoHandler[IpAddress, String](ipAddressIso)

  implicit val emailAddressHandler = isoHandler[EmailAddress, String](emailAddressIso)

  implicit val normalizedEmailAddressHandler = isoHandler[NormalizedEmailAddress, String](normalizedEmailAddressIso)
}
