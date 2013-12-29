package lila.socket

import ornicar.scalalib.Zero
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.json._


trait WithSocket {

  type JsChannel = Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type JsIteratee = Iteratee[JsValue, _]
  type JsSocketHandler = (JsIteratee, JsEnumerator)

  implicit val LilaJsSocketHandlerZero: Zero[JsSocketHandler] =
    Zero.instance(Handler errorHandler "default error handler used")
}
