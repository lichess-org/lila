package lila.db

import chess.variant.Variant
import reactivemongo.api.bson.*
import reactivemongo.api.bson.exceptions.TypeDoesNotMatchException

import scala.util.{ Failure, NotGiven, Success, Try }

import lila.common.Iso.{ *, given }
import lila.core.data.Percent
import lila.core.net.IpAddress
import lila.core.game.Blurs

trait Handlers:

  def toBdoc[A](a: A)(using writer: BSONDocumentWriter[A]): Option[BSONDocument] = writer.writeOpt(a)

  // free writer for all types with TotalWrapper
  // unless they are given an instance of lila.db.NoBSONWriter[T]
  given opaqueWriter[T, A](using
      rs: SameRuntime[T, A],
      writer: BSONWriter[A]
  )(using NotGiven[NoBSONWriter[T]]): BSONWriter[T] with
    def writeTry(t: T) = writer.writeTry(rs(t))

  // free reader for all types with TotalWrapper
  // unless they are given an instance of lila.db.NoBSONReader[T]
  given opaqueReader[T, A](using
      sr: SameRuntime[A, T],
      reader: BSONReader[A]
  )(using NotGiven[NoBSONReader[T]]): BSONReader[T] with
    def readTry(bson: BSONValue) = reader.readTry(bson).map(sr.apply)

  given NoDbHandler[Blurs] with {}

  given userIdOfWriter[U: UserIdOf](using writer: BSONWriter[UserId]): BSONWriter[U] with
    inline def writeTry(u: U) = writer.writeTry(u.id)
  given noUserIdOf[A: lila.core.userId.UserIdOf]: NoBSONWriter[A] with {}

  given NoBSONWriter[UserId] with {}
  given userIdHandler: BSONHandler[UserId] = stringIsoHandler

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
    def readTry(x: BSONValue) = handler.readTry(x).map(iso.from)
    def writeTry(x: A)        = handler.writeTry(iso.to(x))
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
      leftHandler.readTry(bson).map(Left.apply).orElse(rightHandler.readTry(bson).map(Right.apply))
    def writeTry(e: Either[L, R]) = e.fold(leftHandler.writeTry, rightHandler.writeTry)

  given mapHandler[V: BSONHandler]: BSONHandler[Map[String, V]] = new:
    def readTry(bson: BSONValue)    = BSONReader.mapReader.readTry(bson)
    def writeTry(v: Map[String, V]) = BSONWriter.mapWriter.writeTry(v)

  def typedMapHandler[K, V: BSONHandler](using
      sr: SameRuntime[K, String],
      rs: SameRuntime[String, K]
  ): BSONHandler[Map[K, V]] =
    mapHandler[V].as[Map[K, V]](_.mapKeys(rs(_)), _.mapKeys(sr(_)))

  def typedMapHandlerIso[K, V: BSONHandler](using keyIso: StringIso[K]): BSONHandler[Map[K, V]] =
    mapHandler[V].as[Map[K, V]](_.mapKeys(keyIso.from), _.mapKeys(keyIso.to))

  def ifPresentHandler[A](a: A) = quickHandler({ case BSONBoolean(true) => a }, _ => BSONBoolean(true))

  given [T: BSONHandler]: BSONHandler[NonEmptyList[T]] =
    def listWriter = BSONWriter.collectionWriter[T, List[T]]
    def listReader = collectionReader[List, T]
    tryHandler[NonEmptyList[T]](
      { case array: BSONArray =>
        listReader.readTry(array).flatMap {
          _.toNel.toTry(s"BSONArray is empty, can't build NonEmptyList")
        }
      },
      nel => listWriter.writeTry(nel.toList).get
    )

  given BSONWriter[BSONNull.type] with
    def writeTry(n: BSONNull.type) = Success(BSONNull)

  given BSONHandler[IpAddress] = stringIsoHandler

  import lila.core.relation.Relation
  given BSONHandler[Relation] =
    BSONBooleanHandler.as[Relation](if _ then Relation.Follow else Relation.Block, _.isFollow)

  given BSONHandler[Color] = BSONBooleanHandler.as[Color](Color.fromWhite(_), _.white)

  import lila.common.{ LilaOpeningFamily, SimpleOpening }
  given BSONHandler[SimpleOpening] = tryHandler[SimpleOpening](
    { case BSONString(key) => SimpleOpening.find(key).toTry(s"No such opening: $key") },
    o => BSONString(o.key.value)
  )
  given BSONHandler[LilaOpeningFamily] = tryHandler[LilaOpeningFamily](
    { case BSONString(key) => LilaOpeningFamily.find(key).toTry(s"No such opening family: $key") },
    o => BSONString(o.key.value)
  )

  given BSONHandler[chess.Mode] = BSONBooleanHandler.as[chess.Mode](chess.Mode.apply, _.rated)

  given perfKeyHandler: BSONHandler[PerfKey] =
    BSONStringHandler.as[PerfKey](key => PerfKey(key).err(s"Unknown perf key $key"), _.value)

  given perfKeyFailingIso: Iso.StringIso[PerfKey] =
    Iso.string[PerfKey](str => PerfKey(str).err(s"Unknown perf $str"), _.value)

  given [T: BSONHandler]: BSONHandler[(T, T)] = tryHandler[(T, T)](
    { case arr: BSONArray => for a <- arr.getAsTry[T](0); b <- arr.getAsTry[T](1) yield (a, b) },
    { case (a, b) => BSONArray(a, b) }
  )

  given NoDbHandler[chess.Square] with {} // no default opaque handler for chess.Square

  given NoDbHandler[lila.core.user.Me] with {}

  def chessPosKeyHandler: BSONHandler[chess.Square] = tryHandler(
    { case BSONString(str) => chess.Square.fromKey(str).toTry(s"No such key $str") },
    pos => BSONString(pos.key)
  )

  val minutesHandler = BSONIntegerHandler.as[FiniteDuration](_.minutes, _.toMinutes.toInt)

  val variantByKeyHandler: BSONHandler[Variant] = quickHandler[Variant](
    {
      case BSONString(v) => Variant.orDefault(Variant.LilaKey(v))
      case _             => Variant.default
    },
    v => BSONString(v.key.value)
  )
  val variantByIdHandler: BSONHandler[Variant] = tryHandler(
    { case BSONInteger(v) => Variant(Variant.Id(v)).toTry(s"No such variant: $v") },
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

  val langByCodeHandler: BSONHandler[play.api.i18n.Lang] =
    stringAnyValHandler(_.code, play.api.i18n.Lang.apply)

  import chess.PlayerTitle
  given BSONHandler[PlayerTitle] = tryHandler(
    { case BSONString(t) => PlayerTitle.get(t).toTry(s"No such player title: $t") },
    t => BSONString(t.value)
  )

  given BSONHandler[io.mola.galimatias.URL] =
    import io.mola.galimatias.{ StrictErrorHandler, URL, URLParsingSettings }
    val parser = URLParsingSettings.create.withErrorHandler(StrictErrorHandler.getInstance)
    tryHandler(
      { case BSONString(url) => Try(URL.parse(parser, url)) },
      t => BSONString(t.toString)
    )

  import lila.core.user.UserMark
  given markHandler: BSONHandler[UserMark] = valueMapHandler(UserMark.byKey)(_.key)

  def valueMapHandler[K, V](mapping: Map[K, V])(toKey: V => K)(using
      keyHandler: BSONHandler[K]
  ): BSONHandler[V] = new:
    def readTry(bson: BSONValue) = keyHandler.readTry(bson).flatMap { k =>
      mapping.get(k).toTry(s"No such value in mapping: $k")
    }
    def writeTry(v: V) = keyHandler.writeTry(toKey(v))

  def optionPairHandler[A](using handler: BSONHandler[A]): BSONHandler[PairOf[Option[A]]] = quickHandler(
    { case BSONArray(els) => (els.headOption.flatMap(_.asOpt[A]), els.lift(1).flatMap(_.asOpt[A])) },
    { (a, b) => BSONArray(Seq(a.flatMap(handler.writeOpt), b.flatMap(handler.writeOpt)).map(_ | BSONNull)) }
  )
