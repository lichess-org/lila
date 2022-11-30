package controllers

import lila.api.{ *, given }

import play.api.http.*
import play.api.mvc.Codec

trait ResponseWriter:

  private val textContentType = ContentTypeOf(Some(ContentTypes.TEXT))

  given (using codec: Codec): Writeable[Unit] = Writeable(_ => codec encode "ok")
  given ContentTypeOf[Unit]                   = textContentType

  given (using codec: Codec): Writeable[Long] = Writeable(a => codec encode a.toString)
  given ContentTypeOf[Long]                   = textContentType

  given (using codec: Codec): Writeable[Int] = Writeable(i => codec encode i.toString)
  given ContentTypeOf[Int]                   = textContentType

  // given (using codec: Codec): Writeable[Option[String]] = Writeable(i => codec encode i.orZero)
  // given ContentTypeOf[Option[String]]                   = textContentType

  given stringRuntimeWriteable[A](using codec: Codec, sr: StringRuntime[A]): Writeable[A] =
    Writeable(a => codec encode sr(a))
  given stringRuntimeContentType[A: StringRuntime]: ContentTypeOf[A] = textContentType

  given intRuntimeWriteable[A](using codec: Codec, sr: IntRuntime[A]): Writeable[A] =
    Writeable(a => codec encode sr(a).toString)
  given intRuntimeContentType[A: IntRuntime]: ContentTypeOf[A] = textContentType
