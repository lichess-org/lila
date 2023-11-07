package lila.db

import chess.variant.Variant
import reactivemongo.api.bson.*
import reactivemongo.api.bson.exceptions.TypeDoesNotMatchException
import scala.util.{ Failure, Success, Try, NotGiven }

import lila.common.Iso.*
import lila.common.{ EmailAddress, IpAddress, Iso, NormalizedEmailAddress }

trait Handlers:

  // free handlers for all types with TotalWrapper
  // unless they are given an instance of lila.db.NoDbHandler[T]
  given opaqueHandler[T, A](using
      sr: SameRuntime[A, T],
      rs: SameRuntime[T, A],
      handler: BSONHandler[A]
  )(using NotGiven[NoDbHandler[T]]): BSONHandler[T] =
    handler.as(sr.apply, rs.apply)

  given userIdOfWriter[U: UserIdOf, T](using writer: BSONWriter[UserId]): BSONWriter[U] with
    inline def writeTry(u: U) = writer.writeTry(u.id)

  given dateTimeHandler: BSONHandler[LocalDateTime] = quickHandler[LocalDateTime](
    { case v: BSONDateTime => millisToDateTime(v.value) },
    v => BSONDateTime(v.toMillis)
  )
  given instantHandler: BSONHandler[Instant] = quickHandler[Instant](
    { case v: BSONDateTime => millisToInstant(v.value) },
    v => BSONDateTime(v.toMillis)
  )
  given BSONHandler[TimeInterval] = summon[BSONHandler[List[Instant]]].as[TimeInterval](
    list => TimeInterval(list.lift(0).get, list.lift(1).get),
    interval => List(interval.start, interval.end)
  )

  def isoHandler[A, B](using iso: Iso[B, A])(using handler: BSONHandler[B]): BSONHandler[A] = new:
    def readTry(x: BSONValue) = handler.readTry(x) map iso.from
    def writeTry(x: A)        = handler writeTry iso.to(x)
  def isoHandler[A, B](to: A => B, from: B => A)(using handler: BSONHandler[B]): BSONHandler[A] =
    isoHandler(using Iso(from, to))(using handler)

  def stringIsoHandler[A](using iso: StringIso[A]): BSONHandler[A] =
    BSONStringHandler.as[A](iso.from, iso.to)
  def stringAnyValHandler[A](to: A => String, from: String => A): BSONHandler[A] =
    BSONStringHandler.as[A](from, to)

  def intAnyValHandler[A](to: A => Int, from: Int => A): BSONHandler[A] = BSONIntegerHandler.as[A](from, to)

  def booleanIsoHandler[A](using iso: BooleanIso[A]): BSONHandler[A] =
    BSONBooleanHandler.as[A](iso.from, iso.to)
  def booleanAnyValHandler[A](to: A => Boolean, from: Boolean => A): BSONHandler[A] =
    booleanIsoHandler(using Iso(from, to))

  private def doubleAsIntHandler[A](to: A => Double, from: Double => A, multiplier: Int): BSONHandler[A] =
    intAnyValHandler[A](x => Math.round(to(x) * multiplier).toInt, x => from(x.toDouble / multiplier))

  val percentBsonMultiplier = 1000
  val ratioBsonMultiplier   = 100_000

  def percentAsIntHandler[A](using p: Percent[A]): BSONHandler[A] =
    doubleAsIntHandler(p.value, p.apply, percentBsonMultiplier)

  def instantIsoHandler[A](using iso: Iso[Instant, A]): BSONHandler[A] =
    instantHandler.as[A](iso.from, iso.to)

  def quickHandler[T](read: PartialFunction[BSONValue, T], write: T => BSONValue): BSONHandler[T] = new:
    def readTry(bson: BSONValue) =
      read
        .andThen(Success(_))
        .applyOrElse(bson, (b: BSONValue) => handlerBadType(b))
    def writeTry(t: T) = Success(write(t))

  def tryHandler[T](read: PartialFunction[BSONValue, Try[T]], write: T => BSONValue): BSONHandler[T] = new:
    def readTry(bson: BSONValue) =
      read.applyOrElse(
        bson,
        (b: BSONValue) => handlerBadType(b)
      )
    def writeTry(t: T) = Success(write(t))

  def tryReader[T](read: PartialFunction[BSONValue, Try[T]]): BSONReader[T] = new:
    def readTry(bson: BSONValue) = read.applyOrElse(bson, (b: BSONValue) => handlerBadType(b))

  def handlerBadType[T](b: BSONValue): Try[T] =
    Failure(TypeDoesNotMatchException("BSONValue", b.getClass.getSimpleName))

  def handlerBadValue[T](msg: String): Try[T] =
    Failure(new IllegalArgumentException(msg))

  def eitherHandler[L, R](using
      leftHandler: BSONHandler[L],
      rightHandler: BSONHandler[R]
  ): BSONHandler[Either[L, R]] = new:
    def readTry(bson: BSONValue) =
      leftHandler.readTry(bson).map(Left.apply) orElse rightHandler.readTry(bson).map(Right.apply)
    def writeTry(e: Either[L, R]) = e.fold(leftHandler.writeTry, rightHandler.writeTry)

  def stringMapHandler[V](using
      reader: BSONReader[Map[String, V]],
      writer: BSONWriter[Map[String, V]]
  ): BSONHandler[Map[String, V]] = new:
    def readTry(bson: BSONValue)    = reader readTry bson
    def writeTry(v: Map[String, V]) = writer writeTry v

  def typedMapHandler[K, V: BSONHandler](using sr: SameRuntime[K, String], rs: SameRuntime[String, K]) =
    stringMapHandler[V].as[Map[K, V]](_.mapKeys(rs(_)), _.mapKeys(sr(_)))

  def typedMapHandlerIso[K, V: BSONHandler](using keyIso: StringIso[K]) =
    stringMapHandler[V].as[Map[K, V]](_.mapKeys(keyIso.from), _.mapKeys(keyIso.to))

  def ifPresentHandler[A](a: A) = quickHandler({ case BSONBoolean(true) => a }, _ => BSONBoolean(true))

  given [T: BSONHandler]: BSONHandler[NonEmptyList[T]] =
    def listWriter = BSONWriter.collectionWriter[T, List[T]]
    def listReader = collectionReader[List, T]
    tryHandler[NonEmptyList[T]](
      { case array: BSONArray =>
        listReader.readTry(array).flatMap {
          _.toNel toTry s"BSONArray is empty, can't build NonEmptyList"
        }
      },
      nel => listWriter.writeTry(nel.toList).get
    )

  given listHandler[T: BSONHandler]: BSONHandler[List[T]] with
    val reader                                 = collectionReader[List, T]
    val writer                                 = BSONWriter.collectionWriter[T, List[T]]
    def readTry(bson: BSONValue): Try[List[T]] = reader.readTry(bson)
    def writeTry(t: List[T]): Try[BSONValue]   = writer.writeTry(t)

  given vectorHandler[T: BSONHandler]: BSONHandler[Vector[T]] with
    val reader                                   = collectionReader[Vector, T]
    val writer                                   = BSONWriter.collectionWriter[T, Vector[T]]
    def readTry(bson: BSONValue): Try[Vector[T]] = reader.readTry(bson)
    def writeTry(t: Vector[T]): Try[BSONValue]   = writer.writeTry(t)

  given BSONWriter[BSONNull.type] with
    def writeTry(n: BSONNull.type) = Success(BSONNull)

  given BSONHandler[IpAddress] = stringIsoHandler

  given BSONHandler[EmailAddress] = stringIsoHandler

  given BSONHandler[NormalizedEmailAddress] = stringIsoHandler

  given BSONHandler[chess.Color] = BSONBooleanHandler.as[chess.Color](chess.Color.fromWhite(_), _.white)

  import lila.common.{ LilaOpeningFamily, SimpleOpening }
  given BSONHandler[SimpleOpening] = tryHandler[SimpleOpening](
    { case BSONString(key) => SimpleOpening find key toTry s"No such opening: $key" },
    o => BSONString(o.key.value)
  )
  given BSONHandler[LilaOpeningFamily] = tryHandler[LilaOpeningFamily](
    { case BSONString(key) => LilaOpeningFamily find key toTry s"No such opening family: $key" },
    o => BSONString(o.key.value)
  )

  given BSONHandler[chess.Mode] = BSONBooleanHandler.as[chess.Mode](chess.Mode.apply, _.rated)

  given [T: BSONHandler]: BSONHandler[(T, T)] = tryHandler[(T, T)](
    { case arr: BSONArray => for a <- arr.getAsTry[T](0); b <- arr.getAsTry[T](1) yield (a, b) },
    { case (a, b) => BSONArray(a, b) }
  )

  given NoDbHandler[chess.Square] with {} // no default opaque handler for chess.Square

  def chessPosKeyHandler: BSONHandler[chess.Square] = tryHandler(
    { case BSONString(str) => chess.Square.fromKey(str) toTry s"No such key $str" },
    pos => BSONString(pos.key)
  )

  val minutesHandler = BSONIntegerHandler.as[FiniteDuration](_.minutes, _.toMinutes.toInt)

  val variantByKeyHandler: BSONHandler[Variant] = quickHandler[Variant](
    {
      case BSONString(v) => Variant orDefault Variant.LilaKey(v)
      case _             => Variant.default
    },
    v => BSONString(v.key.value)
  )
  val variantByIdHandler: BSONHandler[Variant] = tryHandler(
    { case BSONInteger(v) => Variant(Variant.Id(v)) toTry s"No such variant: $v" },
    x => BSONInteger(x.id.value)
  )

  val clockConfigHandler = tryHandler[chess.Clock.Config](
    { case doc: BSONDocument =>
      import chess.Clock.*
      for
        limit <- doc.getAsTry[LimitSeconds]("limit")
        inc   <- doc.getAsTry[IncrementSeconds]("increment")
      yield Config(limit, inc)
    },
    c =>
      BSONDocument(
        "limit"     -> c.limitSeconds,
        "increment" -> c.incrementSeconds
      )
  )

  def valueMapHandler[K, V](mapping: Map[K, V])(toKey: V => K)(using
      keyHandler: BSONHandler[K]
  ): BSONHandler[V] = new:
    def readTry(bson: BSONValue) = keyHandler.readTry(bson) flatMap { k =>
      mapping.get(k) toTry s"No such value in mapping: $k"
    }
    def writeTry(v: V) = keyHandler writeTry toKey(v)
