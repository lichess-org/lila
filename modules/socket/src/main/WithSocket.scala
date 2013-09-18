package lila.socket

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.json._

trait WithSocket {

  type JsChannel = Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type JsSocketHandler = (Iteratee[JsValue, _], JsEnumerator)

  val defaultSocketHandler = Handler errorHandler "default error handler used"
}
