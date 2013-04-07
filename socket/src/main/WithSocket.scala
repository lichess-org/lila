package lila.socket

import play.api.libs.json._
import play.api.libs.concurrent.Promise
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.iteratee.Concurrent.Channel
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import scalaz.{ Zero, Zeros }

trait WithSocket extends Zeros {

  type JsChannel = Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type SocketHandler = (Iteratee[JsValue, _], JsEnumerator)

  implicit val LilaSocketHandlerZero = new Zero[SocketHandler] {

    val zero: SocketHandler =
      Iteratee.skipToEof[JsValue] ->
        Enumerator[JsValue](Json.obj("error" -> "Invalid request"))
        .andThen(Enumerator.eof)
  }
}
