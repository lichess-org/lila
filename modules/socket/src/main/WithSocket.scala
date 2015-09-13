package lila.socket

import ornicar.scalalib.Zero
import play.api.libs.iteratee.{ Iteratee, Enumerator }
import play.api.libs.json._


trait WithSocket {

  type JsChannel = play.api.libs.iteratee.Concurrent.Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type JsIteratee = Iteratee[JsValue, _]
  type JsSocketHandler = (JsIteratee, JsEnumerator)
}
