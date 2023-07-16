package lila.common

import play.api.libs.json.{ Json as PlayJson, * }
import chess.format.{ Uci }
import scala.util.NotGiven
import chess.variant.Crazyhouse

object Json:

  trait NoJsonHandler[A] // don't create default JSON handlers for this type

  given opaqueFormat[A, T](using
      bts: SameRuntime[A, T],
      stb: SameRuntime[T, A],
      format: Format[A]
  )(using NotGiven[NoJsonHandler[T]]): Format[T] =
    format.bimap(bts.apply, stb.apply)

  // given yesnoFormat[T](using
  //     bts: SameRuntime[Boolean, T],
  //     stb: SameRuntime[T, Boolean],
  //     format: Format[Boolean]
  // ): Format[T] =
  //   format.bimap(bts.apply, stb.apply)

  given [A](using bts: SameRuntime[A, String]): KeyWrites[A] with
    def writeKey(key: A) = bts(key)

  private val stringFormatBase: Format[String] = Format(Reads.StringReads, Writes.StringWrites)
  private val intFormatBase: Format[Int]       = Format(Reads.IntReads, Writes.IntWrites)

  def stringFormat[A <: String](f: String => A): Format[A] = stringFormatBase.bimap(f, identity)
  def intFormat[A <: Int](f: Int => A): Format[A]          = intFormatBase.bimap(f, identity)

  def writeAs[O, A: Writes](f: O => A) = Writes[O](o => PlayJson toJson f(o))

  def writeWrap[A, B](fieldName: String)(get: A => B)(using writes: Writes[B]): OWrites[A] = OWrites { a =>
    PlayJson.obj(fieldName -> writes.writes(get(a)))
  }

  def stringIsoWriter[O](using iso: Iso[String, O]): Writes[O] = writeAs[O, String](iso.to)
  def intIsoWriter[O](using iso: Iso[Int, O]): Writes[O]       = writeAs[O, Int](iso.to)

  def stringIsoReader[O](using iso: Iso[String, O]): Reads[O] = Reads.of[String] map iso.from

  def intIsoFormat[O](using iso: Iso[Int, O]): Format[O] =
    Format[O](
      Reads.of[Int] map iso.from,
      Writes: o =>
        JsNumber(iso to o)
    )

  def stringIsoFormat[O](using iso: Iso[String, O]): Format[O] =
    Format[O](
      Reads.of[String] map iso.from,
      Writes: o =>
        JsString(iso to o)
    )

  def stringRead[O](from: String => O): Reads[O] = Reads.of[String] map from

  def optRead[O](from: String => Option[O]): Reads[O] = Reads.of[String].flatMapResult { str =>
    from(str).fold[JsResult[O]](JsError(s"Invalid value: $str"))(JsSuccess(_))
  }
  def optFormat[O](from: String => Option[O], to: O => String): Format[O] = Format[O](
    optRead(from),
    Writes(o => JsString(to(o)))
  )

  def tryRead[O](from: String => scala.util.Try[O]): Reads[O] = Reads.of[String].flatMapResult { code =>
    from(code).fold(err => JsError(err.getMessage), JsSuccess(_))
  }
  def tryFormat[O](from: String => scala.util.Try[O], to: O => String): Format[O] = Format[O](
    tryRead(from),
    Writes[O](o => JsString(to(o)))
  )

  given userStrReads: Reads[UserStr] = Reads.of[String] flatMapResult { str =>
    JsResult.fromTry(UserStr.read(str) toTry s"Invalid username: $str")
  }

  given Writes[Instant] = writeAs(_.toMillis)

  given Writes[chess.Color] = writeAs(_.name)

  given Reads[Uci] = Reads.of[String] flatMapResult { str =>
    JsResult.fromTry(Uci(str) toTry s"Invalid UCI: $str")
  }
  given Writes[Uci] = writeAs(_.uci)

  given Reads[LilaOpeningFamily] = Reads[LilaOpeningFamily]: f =>
    f.get[String]("key")
      .flatMap(LilaOpeningFamily.find)
      .fold[JsResult[LilaOpeningFamily]](JsError(Nil))(JsSuccess(_))

  given NoJsonHandler[chess.Square] with {}

  given OWrites[Crazyhouse.Pocket] = OWrites: p =>
    JsObject:
      p.flatMap((role, nb) => Option.when(nb > 0)(role.name -> JsNumber(nb)))

  given OWrites[chess.variant.Crazyhouse.Data] = OWrites: v =>
    PlayJson.obj("pockets" -> v.pockets.all)
