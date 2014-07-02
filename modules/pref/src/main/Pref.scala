package lila.pref

import play.api.libs.json._

import lila.db.JsTube
import lila.db.JsTube.Helpers._
import lila.user.User

case class Pref(
    id: String, // user id
    dark: Boolean,
    theme: String,
    pieceSet: String,
    autoQueen: Int,
    autoThreefold: Int,
    takeback: Int,
    clockTenths: Boolean,
    clockBar: Boolean,
    premove: Boolean,
    animation: Int,
    captured: Boolean,
    follow: Boolean,
    highlight: Boolean,
    challenge: Int,
    coordColor: Int,
    puzzleDifficulty: Int,
    tags: Map[String, String] = Map.empty) {

  import Pref._

  def realTheme = Theme(theme)
  def realPieceSet = PieceSet(theme)

  def coordColorName = Color.choices.toMap.get(coordColor).fold("random")(_.toLowerCase)

  def hasSeenVerifyTitle = tags contains Tag.verifyTitle

  def get(name: String): Option[String] = name match {
    case "bg"       => dark.fold("dark", "light").some
    case "theme"    => theme.some
    case "pieceSet" => pieceSet.some
    case _          => none
  }
  def set(name: String, value: String): Option[Pref] = name match {
    case "bg"       => Pref.bgs get value map { b => copy(dark = b) }
    case "theme"    => Theme.allByName get value map { t => copy(theme = t.name) }
    case "pieceSet" => PieceSet.allByName get value map { p => copy(pieceSet = p.name) }
    case _          => none
  }
}

object Pref {

  object Tag {
    val verifyTitle = "verifyTitle"
  }

  object Difficulty {
    val EASY = 1
    val NORMAL = 2
    val HARD = 3

    val choices = Seq(
      EASY -> "Easy",
      NORMAL -> "Normal",
      HARD -> "Hard")
  }

  object Color {
    val WHITE = 1
    val RANDOM = 2
    val BLACK = 3

    val choices = Seq(
      WHITE -> "White",
      RANDOM -> "Random",
      BLACK -> "Black")
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

  object Challenge {
    val NEVER = 1
    val RATING = 2
    val FRIEND = 3
    val ALWAYS = 4

    private val ratingThreshold = 500

    val choices = Seq(
      NEVER -> "Never",
      RATING -> s"If rating is ± $ratingThreshold",
      FRIEND -> "Only friends",
      ALWAYS -> "Always")

    def block(from: User, to: User, pref: Int, follow: Boolean): Option[String] = pref match {
      case NEVER => "{{user}} doesn't accept accept challenges.".some
      case RATING if math.abs(from.rating - to.rating) > ratingThreshold =>
        s"{{user}} only accepts challenges if rating is ± $ratingThreshold.".some
      case FRIEND if !follow => "{{user}} only accepts challenges from friends.".some
      case _                 => none
    }
  }

  def create(id: String) = Pref(
    id = id,
    dark = false,
    theme = Theme.default.name,
    pieceSet = PieceSet.default.name,
    autoQueen = AutoQueen.PREMOVE,
    autoThreefold = AutoThreefold.TIME,
    takeback = Takeback.ALWAYS,
    clockTenths = true,
    clockBar = true,
    premove = true,
    animation = 2,
    captured = true,
    follow = true,
    highlight = true,
    challenge = Challenge.RATING,
    coordColor = Color.RANDOM,
    puzzleDifficulty = Difficulty.NORMAL,
    tags = Map.empty)

  val default = create("")

  private val booleans = Map("true" -> true, "false" -> false)
  private val bgs = Map("light" -> false, "dark" -> true)

  private[pref] lazy val tube = JsTube[Pref](
    (__.json update merge(defaults)) andThen Json.reads[Pref],
    Json.writes[Pref])

  private def defaults = Json.obj(
    "dark" -> default.dark,
    "theme" -> default.theme,
    "pieceSet" -> default.pieceSet,
    "autoQueen" -> default.autoQueen,
    "autoThreefold" -> default.autoThreefold,
    "takeback" -> default.takeback,
    "clockTenths" -> default.clockTenths,
    "clockBar" -> default.clockBar,
    "premove" -> default.premove,
    "animation" -> default.animation,
    "captured" -> default.captured,
    "follow" -> default.follow,
    "highlight" -> default.highlight,
    "challenge" -> default.challenge,
    "coordColor" -> default.coordColor,
    "puzzleDifficulty" -> default.puzzleDifficulty,
    "tags" -> default.tags)
}
