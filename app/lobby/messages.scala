package lila
package lobby

import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue

case class Entry(entry: model.Entry)
case class Join(uid: String)
case class Quit(uid: String)
case class Talk(txt: String, u: String)
case class Connected(enumerator: Enumerator[JsValue])
