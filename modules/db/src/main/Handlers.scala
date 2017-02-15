package lila.db

import org.joda.time.DateTime
import reactivemongo.bson._
import scalaz.NonEmptyList

import lila.common.Iso
import lila.common.Iso._

trait Handlers {

  implicit object BSONJodaDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(x: BSONDateTime) = new DateTime(x.value)
    def write(x: DateTime) = BSONDateTime(x.getMillis)
  }

  def isoHandler[A, B, C <: BSONValue](iso: Iso[B, A])(implicit handler: BSONHandler[C, B]): BSONHandler[C, A] = new BSONHandler[C, A] {
    def read(x: C): A = iso.from(handler read x)
    def write(x: A): C = handler write iso.to(x)
  }
  def isoHandler[A, B, C <: BSONValue](to: A => B, from: B => A)(implicit handler: BSONHandler[C, B]): BSONHandler[C, A] =
    isoHandler(Iso(from, to))

  def stringIsoHandler[A](implicit iso: StringIso[A]): BSONHandler[BSONString, A] = isoHandler[A, String, BSONString](iso)
  def stringAnyValHandler[A](to: A => String, from: String => A): BSONHandler[BSONString, A] = stringIsoHandler(Iso(from, to))

  def intIsoHandler[A](implicit iso: IntIso[A]): BSONHandler[BSONInteger, A] = isoHandler[A, Int, BSONInteger](iso)
  def intAnyValHandler[A](to: A => Int, from: Int => A): BSONHandler[BSONInteger, A] = intIsoHandler(Iso(from, to))

  def booleanIsoHandler[A](implicit iso: BooleanIso[A]): BSONHandler[BSONBoolean, A] = isoHandler[A, Boolean, BSONBoolean](iso)
  def booleanAnyValHandler[A](to: A => Boolean, from: Boolean => A): BSONHandler[BSONBoolean, A] = booleanIsoHandler(Iso(from, to))

  def doubleIsoHandler[A](implicit iso: DoubleIso[A]): BSONHandler[BSONDouble, A] = isoHandler[A, Double, BSONDouble](iso)
  def doubleAnyValHandler[A](to: A => Double, from: Double => A): BSONHandler[BSONDouble, A] = doubleIsoHandler(Iso(from, to))

  def dateIsoHandler[A](implicit iso: Iso[DateTime, A]): BSONHandler[BSONDateTime, A] = isoHandler[A, DateTime, BSONDateTime](iso)

  implicit def bsonArrayToListHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]): BSONHandler[BSONArray, List[T]] = new BSONHandler[BSONArray, List[T]] {
    def read(array: BSONArray) = readStream(array, reader.asInstanceOf[BSONReader[BSONValue, T]]).toList
    def write(repr: List[T]) =
      new BSONArray(repr.map(s => scala.util.Try(writer.write(s))).to[Stream])
  }

  implicit def bsonArrayToVectorHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]): BSONHandler[BSONArray, Vector[T]] = new BSONHandler[BSONArray, Vector[T]] {
    def read(array: BSONArray) = readStream(array, reader.asInstanceOf[BSONReader[BSONValue, T]]).toVector
    def write(repr: Vector[T]) =
      new BSONArray(repr.map(s => scala.util.Try(writer.write(s))).to[Stream])
  }

  implicit def bsonArrayToNonEmptyListHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]): BSONHandler[BSONArray, NonEmptyList[T]] = new BSONHandler[BSONArray, NonEmptyList[T]] {
    private val listHandler = bsonArrayToListHandler[T]
    def read(array: BSONArray) = listHandler.read(array).toNel err s"BSONArray is empty, can't build NonEmptyList"
    def write(repr: NonEmptyList[T]) = listHandler.write(repr.list)
  }

  private def readStream[T](array: BSONArray, reader: BSONReader[BSONValue, T]): Stream[T] = {
    array.stream.filter(_.isSuccess).map { v =>
      reader.read(v.get)
    }
  }
}
