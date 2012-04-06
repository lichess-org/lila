package lila

import play.api.libs.json.JsValue

package object lobby {

  type Channel = LilaEnumerator[JsValue]
}
