package lila.db

import cats.data.NonEmptyList
import org.joda.time.DateTime
import reactivemongo.api.bson._
import reactivemongo.api.bson.exceptions.TypeDoesNotMatchException
import scala.util.{ Failure, Success, Try }

import lila.common.Iso._
import lila.common.{ EmailAddress, IpAddress, Iso, NormalizedEmailAddress }
import chess.format.FEN
import chess.variant.Variant
import io.lemonlabs.uri.AbsoluteUrl

trait Handlers {

  implicit val BSONJodaDateTimeHandler = quickHandler[DateTime](
    { case v: BSONDateTime => new DateTime(v.value) },
    v => BSONDateTime(v.getMillis)
  )

  def isoHandler[A, B](iso: Iso[B, A])(implicit handler: BSONHandler[B]): BSONHandler[A] =
    new BSONHandler[A] {
      def readTry(x: BSONValue) = handler.readTry(x) map iso.from
      def writeTry(x: A)        = handler writeTry iso.to(x)
    }
  def isoHandler[A, B](to: A => B, from: B => A)(implicit handler: BSONHandler[B]): BSONHandler[A] =
    isoHandler(Iso(from, to))

  def stringIsoHandler[A](implicit iso: StringIso[A]): BSONHandler[A] =
    BSONStringHandler.as[A](iso.from, iso.to)
  def stringAnyValHandler[A](to: A => String, from: String => A): BSONHandler[A] =
    stringIsoHandler(Iso(from, to))

  def intIsoHandler[A](implicit iso: IntIso[A]): BSONHandler[A]         = BSONIntegerHandler.as[A](iso.from, iso.to)
  def intAnyValHandler[A](to: A => Int, from: Int => A): BSONHandler[A] = intIsoHandler(Iso(from, to))

  def booleanIsoHandler[A](implicit iso: BooleanIso[A]): BSONHandler[A] =
    BSONBooleanHandler.as[A](iso.from, iso.to)
  def booleanAnyValHandler[A](to: A => Boolean, from: Boolean => A): BSONHandler[A] =
    booleanIsoHandler(Iso(from, to))

  def doubleIsoHandler[A](implicit iso: DoubleIso[A]): BSONHandler[A] =
    BSONDoubleHandler.as[A](iso.from, iso.to)
  def doubleAnyValHandler[A](to: A => Double, from: Double => A): BSONHandler[A] =
    doubleIsoHandler(Iso(from, to))

  def floatIsoHandler[A](implicit iso: FloatIso[A]): BSONHandler[A] =
    BSONFloatHandler.as[A](iso.from, iso.to)
  def floatAnyValHandler[A](to: A => Float, from: Float => A): BSONHandler[A] =
    floatIsoHandler(Iso(from, to))

  def bigDecimalIsoHandler[A](implicit iso: BigDecimalIso[A]): BSONHandler[A] =
    BSONDecimalHandler.as[A](iso.from, iso.to)

  def bigDecimalAnyValHandler[A](to: A => BigDecimal, from: BigDecimal => A): BSONHandler[A] =
    bigDecimalIsoHandler(Iso(from, to))

  def dateIsoHandler[A](implicit iso: Iso[DateTime, A]): BSONHandler[A] =
    BSONJodaDateTimeHandler.as[A](iso.from, iso.to)

  def quickHandler[T](read: PartialFunction[BSONValue, T], write: T => BSONValue): BSONHandler[T] =
    new BSONHandler[T] {
      def readTry(bson: BSONValue) =
        read
          .andThen(Success(_))
          .applyOrElse(bson, (b: BSONValue) => handlerBadType(b))
      def writeTry(t: T) = Success(write(t))
    }

  def tryHandler[T](read: PartialFunction[BSONValue, Try[T]], write: T => BSONValue): BSONHandler[T] =
    new BSONHandler[T] {
      def readTry(bson: BSONValue) =
        read.applyOrElse(
          bson,
          (b: BSONValue) => handlerBadType(b)
        )
      def writeTry(t: T) = Success(write(t))
    }

  def handlerBadType[T](b: BSONValue): Try[T] =
    Failure(TypeDoesNotMatchException("BSONValue", b.getClass.getSimpleName))

  def handlerBadValue[T](msg: String): Try[T] =
    Failure(new IllegalArgumentException(msg))

  def stringMapHandler[V](implicit
      reader: BSONReader[Map[String, V]],
      writer: BSONWriter[Map[String, V]]
  ) =
    new BSONHandler[Map[String, V]] {
      def readTry(bson: BSONValue)    = reader readTry bson
      def writeTry(v: Map[String, V]) = writer writeTry v
    }

  def typedMapHandler[K, V: BSONReader: BSONWriter](keyIso: StringIso[K]) =
    stringMapHandler[V].as[Map[K, V]](
      _.map { case (k, v) => keyIso.from(k) -> v },
      _.map { case (k, v) => keyIso.to(k) -> v }
    )

  implicit def bsonArrayToNonEmptyListHandler[T](implicit handler: BSONHandler[T]) = {
    def listWriter = collectionWriter[T, List[T]]
    def listReader = collectionReader[List, T]
    tryHandler[NonEmptyList[T]](
      { case array: BSONArray =>
        listReader.readTry(array).flatMap {
          _.toNel toTry s"BSONArray is empty, can't build NonEmptyList"
        }
      },
      nel => listWriter.writeTry(nel.toList).get
    )
  }

  implicit object BSONNullWriter extends BSONWriter[BSONNull.type] {
    def writeTry(n: BSONNull.type) = Success(BSONNull)
  }

  implicit val ipAddressHandler = isoHandler[IpAddress, String](ipAddressIso)

  implicit val emailAddressHandler = isoHandler[EmailAddress, String](emailAddressIso)

  implicit val normalizedEmailAddressHandler =
    isoHandler[NormalizedEmailAddress, String](normalizedEmailAddressIso)

  implicit val colorBoolHandler = BSONBooleanHandler.as[chess.Color](chess.Color.fromWhite, _.white)

  implicit val FENHandler: BSONHandler[FEN] = stringAnyValHandler[FEN](_.value, FEN.apply)

  implicit val modeHandler = BSONBooleanHandler.as[chess.Mode](chess.Mode.apply, _.rated)

  val variantByKeyHandler: BSONHandler[Variant] = quickHandler[Variant](
    {
      case BSONString(v) => Variant orDefault v
      case _             => Variant.default
    },
    v => BSONString(v.key)
  )

  val clockConfigHandler = tryHandler[chess.Clock.Config](
    { case doc: BSONDocument =>
      for {
        limit <- doc.getAsTry[Int]("limit")
        inc   <- doc.getAsTry[Int]("increment")
      } yield chess.Clock.Config(limit, inc)
    },
    c =>
      BSONDocument(
        "limit"     -> c.limitSeconds,
        "increment" -> c.incrementSeconds
      )
  )

  implicit val absoluteUrlHandler = tryHandler[AbsoluteUrl](
    { case str: BSONString => AbsoluteUrl parseTry str.value },
    url => BSONString(url.toString)
  )
}
