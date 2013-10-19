package lila.pref

import scala._

case class Pref(
    id: String, // user id
    dark: Boolean,
    chat: Boolean,
    sound: Boolean,
    theme: String) {

  def realTheme = Theme(theme)
  def toggleDark = copy(dark = !dark)
  def toggleChat = copy(chat = !chat)
  def toggleSound = copy(sound = !sound)

  def get(name: String): Option[String] = name match {
    case "dark"  ⇒ dark.toString.some
    case "chat"  ⇒ chat.toString.some
    case "sound" ⇒ sound.toString.some
    case "theme" ⇒ theme.some
    case _       ⇒ none
  }
  def set(name: String, value: String): Option[Pref] = name match {
    case "dark"  ⇒ Pref.booleans get value map { b ⇒ copy(dark = b) }
    case "chat"  ⇒ Pref.booleans get value map { b ⇒ copy(chat = b) }
    case "sound" ⇒ Pref.booleans get value map { b ⇒ copy(sound = b) }
    case "theme" ⇒ Theme.allByName get value map { t ⇒ copy(theme = t.name) }
    case _       ⇒ none
  }
}

object Pref {

  val default = Pref(
    id = "",
    dark = false,
    chat = true,
    sound = false,
    theme = Theme.default.name)

  private val booleans = Map("true" -> true, "false" -> false)

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private[pref] lazy val tube = Tube[Pref](
    (__.json update merge(defaults)) andThen Json.reads[Pref],
    Json.writes[Pref]
  )

  private def defaults = Json.obj(
    "dark" -> default.dark,
    "chat" -> default.chat,
    "sound" -> default.sound,
    "theme" -> default.theme)
}
