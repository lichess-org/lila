package lila.pref

import scala._

case class Pref(
    id: String, // user id
    dark: Boolean,
    theme: String,
    autoQueen: Int,
    clockTenths: Boolean,
    premove: Boolean) {

  def realTheme = Theme(theme)

  def get(name: String): Option[String] = name match {
    case "bg"    ⇒ dark.fold("dark", "light").some
    case "theme" ⇒ theme.some
    case _       ⇒ none
  }
  def set(name: String, value: String): Option[Pref] = name match {
    case "bg"    ⇒ Pref.bgs get value map { b ⇒ copy(dark = b) }
    case "theme" ⇒ Theme.allByName get value map { t ⇒ copy(theme = t.name) }
    case _       ⇒ none
  }
}

object Pref {

  object AutoQueen {
    val NEVER = 1
    val PREMOVE = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Always choose manually",
      PREMOVE -> "Automatic queen on premove",
      ALWAYS -> "Always automatic queen")
  }

  def create(id: String) = Pref(
    id = id,
    dark = false,
    theme = Theme.default.name,
    autoQueen = AutoQueen.PREMOVE,
    clockTenths = true,
    premove = true)

  val default = create("")

  private val booleans = Map("true" -> true, "false" -> false)
  private val bgs = Map("light" -> false, "dark" -> true)

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private[pref] lazy val tube = JsTube[Pref](
    (__.json update merge(defaults)) andThen Json.reads[Pref],
    Json.writes[Pref]
  )

  private def defaults = Json.obj(
    "dark" -> default.dark,
    "theme" -> default.theme,
    "autoQueen" -> default.autoQueen,
    "clockTenths" -> default.clockTenths,
    "premove" -> default.premove)
}
