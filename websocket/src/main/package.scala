package lila

import play.api.libs.iteratee._
import play.api.libs.json._

package object websocket extends PackageObject with WithPlay {

  val connectionFail: SocketFuture = fuccess {
    Done[JsValue, Unit]((), Input.EOF) -> (Enumerator[JsValue](
      JsObject(Seq("error" -> JsString("Invalid request")))
    ) andThen Enumerator.enumInput(Input.EOF))
  }
}
