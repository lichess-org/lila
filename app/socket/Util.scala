package lila.app
package socket

import play.api.libs.concurrent._
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.concurrent.Future

object Util {

  val connectionFail: SocketFuture = Future successful {
    Done[JsValue, Unit]((), Input.EOF) -> (Enumerator[JsValue](
      JsObject(Seq("error" -> JsString("Invalid request")))
    ) andThen Enumerator.enumInput(Input.EOF))
  }
}
