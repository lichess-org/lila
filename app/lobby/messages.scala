package lila
package lobby

import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue

case class AddHook(hook: model.Hook)
case class RemoveHook(hook: model.Hook)
case class Entry(entry: model.Entry)
case class Join(uid: String, version: Int)
case class Quit(uid: String)
case class Talk(txt: String, u: String)
case class Connected(enumerator: Enumerator[JsValue])
