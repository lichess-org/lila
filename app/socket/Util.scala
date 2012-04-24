package lila
package socket

import play.api.libs.concurrent._
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.util.Random

object Util {

  val connectionFail: SocketPromise = Promise.pure {
    Done[JsValue, Unit]((), Input.EOF) -> (Enumerator[JsValue](
      JsObject(Seq("error" -> JsString("Invalid request")))
    ) andThen Enumerator.enumInput(Input.EOF))
  }
}
