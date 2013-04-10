package lila.socket

import play.api.libs.json._
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.iteratee.Concurrent.Channel
import scalaz.{ Zero, Zeros }

trait WithSocket extends Zeros {

  type JsChannel = Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type JsSocketHandler = (Iteratee[JsValue, _], JsEnumerator)

  implicit val LilaJsSocketHandlerZero = new Zero[JsSocketHandler] {

    val zero: JsSocketHandler = Handler errorHandler "default error handler used"
  }
}
