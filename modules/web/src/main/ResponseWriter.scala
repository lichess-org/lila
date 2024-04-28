package lila.web

import chess.format.pgn.PgnStr
import play.api.http.*
import play.api.mvc.Codec
import scalatags.Text.Frag

trait ResponseWriter extends ContentTypes:

  private val textContentType = ContentTypeOf(Some(ContentTypes.TEXT))

  given ContentTypeOf[Unit]                   = textContentType
  given (using codec: Codec): Writeable[Unit] = Writeable(_ => codec.encode("ok"))

  given ContentTypeOf[Long]                   = textContentType
  given (using codec: Codec): Writeable[Long] = Writeable(a => codec.encode(a.toString))

  given ContentTypeOf[Int]                   = textContentType
  given (using codec: Codec): Writeable[Int] = Writeable(i => codec.encode(i.toString))

  val pgnContentType = "application/x-chess-pgn"
  given pgnWriteable(using codec: Codec): Writeable[PgnStr] =
    Writeable(p => codec.encode(p.toString), pgnContentType.some)

  // given (using codec: Codec): Writeable[Option[String]] = Writeable(i => codec encode i.orZero)
  // given ContentTypeOf[Option[String]]                   = textContentType

  given stringRuntimeContentType[A: StringRuntime]: ContentTypeOf[A] = textContentType
  given stringRuntimeWriteable[A](using codec: Codec, sr: StringRuntime[A]): Writeable[A] =
    Writeable(a => codec.encode(sr(a)))

  given intRuntimeContentType[A: IntRuntime]: ContentTypeOf[A] = textContentType
  given intRuntimeWriteable[A](using codec: Codec, sr: IntRuntime[A]): Writeable[A] =
    Writeable(a => codec.encode(sr(a).toString))

  given (using codec: Codec): ContentTypeOf[lila.ui.Page] = ContentTypeOf(Some(ContentTypes.HTML))
  given (using codec: Codec): ContentTypeOf[Frag]         = ContentTypeOf(Some(ContentTypes.HTML))
  given (using codec: Codec): Writeable[Frag]             = Writeable(frag => codec.encode(frag.render))

  val csvContentType = "text/csv"

  object ndJson:
    import akka.stream.scaladsl.Source
    import play.api.libs.json.{ Json, JsValue }

    val contentType = "application/x-ndjson"

    def addKeepAlive(source: Source[JsValue, ?]): Source[Option[JsValue], ?] =
      source
        .map(some)
        .keepAlive(50.seconds, () => none) // play's idleTimeout = 75s

    def jsToString(source: Source[JsValue, ?]) =
      source.map: o =>
        Json.stringify(o) + "\n"

    def jsOptToString(source: Source[Option[JsValue], ?]) =
      source.map:
        _.so(Json.stringify) + "\n"
