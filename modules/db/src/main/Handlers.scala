package lila.db

import org.joda.time.DateTime
import reactivemongo.bson._
import scalaz.NonEmptyList

trait Handlers {

  implicit object BSONJodaDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(x: BSONDateTime) = new DateTime(x.value)
    def write(x: DateTime) = BSONDateTime(x.getMillis)
  }

  def stringAnyValHandler[A](from: A => String, to: String => A) = new BSONHandler[BSONString, A] {
    def read(x: BSONString) = to(x.value)
    def write(x: A) = BSONString(from(x))
  }

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
