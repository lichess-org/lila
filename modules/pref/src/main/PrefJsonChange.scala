package lila.pref

import play.api.libs.json.*

object PrefJsonChange:

  case class Change(name: String, read: Pref => Option[JsValue], write: (Pref, JsValue) => Pref)

  private def notNull(js: JsValue) = Option.unless(js == JsNull)(js)

  val changes = List(
    Change("lobbyShortcuts", _.lobbyShortcuts, (pref, js) => pref.copy(lobbyShortcuts = notNull(js)))
  )

  def apply(name: String) = changes.find(_.name == name)
