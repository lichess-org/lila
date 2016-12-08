package lila.socket

import play.api.libs.iteratee.{ Iteratee, Enumerator, Concurrent }
import play.api.libs.json.JsValue

trait WithSocket {

  type JsChannel = Concurrent.Channel[JsValue]
  type JsEnumerator = Enumerator[JsValue]
  type JsIteratee = Iteratee[JsValue, _]
  type JsSocketHandler = (JsIteratee, JsEnumerator)
}
