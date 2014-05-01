package lila.pref

import play.api.libs.json._

import lila.db.JsTube
import lila.db.JsTube.Helpers._

case class Pref(
    id: String, // user id
    dark: Boolean,
    theme: String,
    autoQueen: Int,
    autoThreefold: Int,
    takeback: Int,
    clockTenths: Boolean,
    clockBar: Boolean,
    premove: Boolean,
    puzzleDifficulty: Int) {

  import Pref._

  def realTheme = Theme(theme)

  def get(name: String): Option[String] = name match {
    case "bg"    => dark.fold("dark", "light").some
    case "theme" => theme.some
    case _       => none
  }
  def set(name: String, value: String): Option[Pref] = name match {
    case "bg"    => Pref.bgs get value map { b => copy(dark = b) }
    case "theme" => Theme.allByName get value map { t => copy(theme = t.name) }
    case _       => none
  }
}

object Pref {

  object Difficulty {
    val EASY = 1
    val NORMAL = 2
    val HARD = 3

    val choices = Seq(
      EASY -> "Easy",
      NORMAL -> "Normal",
      HARD -> "Hard")
  }

  object AutoQueen {
    val NEVER = 1
    val PREMOVE = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      PREMOVE -> "When premoving")
  }

  object AutoThreefold {
    val NEVER = 1
    val TIME = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      TIME -> "When time remaining < 30 seconds")
  }

  object Takeback {
    val NEVER = 1
    val CASUAL = 2
    val ALWAYS = 3

    val choices = Seq(
      NEVER -> "Never",
      ALWAYS -> "Always",
      CASUAL -> "In casual games only")
  }

  def create(id: String) = Pref(
    id = id,
    dark = false,
    theme = Theme.default.name,
    autoQueen = AutoQueen.PREMOVE,
    autoThreefold = AutoThreefold.TIME,
    takeback = Takeback.ALWAYS,
    clockTenths = true,
    clockBar = true,
    premove = true,
    puzzleDifficulty = Difficulty.NORMAL)

  val default = create("")

  private val booleans = Map("true" -> true, "false" -> false)
  private val bgs = Map("light" -> false, "dark" -> true)

  private[pref] lazy val tube = JsTube[Pref](
    (__.json update merge(defaults)) andThen Json.reads[Pref],
    Json.writes[Pref])

  private def defaults = Json.obj(
    "dark" -> default.dark,
    "theme" -> default.theme,
    "autoQueen" -> default.autoQueen,
    "autoThreefold" -> default.autoThreefold,
    "takeback" -> default.takeback,
    "clockTenths" -> default.clockTenths,
    "clockBar" -> default.clockBar,
    "premove" -> default.premove,
    "puzzleDifficulty" -> default.puzzleDifficulty)
}
