package lidraughts.simul
package crud

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import draughts.StartingPosition
import lidraughts.common.Form._

object CrudForm {

  import SimulForm._
  import lidraughts.common.Form.UTCDate._

  val maxHomepageHours = 336

  lazy val apply = Form(mapping(
    "name" -> text(minLength = 3, maxLength = 40),
    "homepageHours" -> number(min = 0, max = maxHomepageHours),
    "date" -> utcDate,
    "image" -> stringIn(imageChoices),
    "headline" -> text(minLength = 5, maxLength = 30),
    "description" -> text(minLength = 10, maxLength = 800),
    "hostName" -> lidraughts.user.DataForm.historicalUsernameField,
    "arbiterName" -> text(minLength = 0, maxLength = 30),
    "arbiterHidden" -> boolean,
    "clockTime" -> numberIn(moderatorClockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "clockExtra" -> numberIn(clockExtraChoices),
    "variants" -> list {
      number.verifying(Set(draughts.variant.Standard.id, draughts.variant.Frisian.id, draughts.variant.Frysk.id, draughts.variant.Antidraughts.id, draughts.variant.Breakthrough.id, draughts.variant.Russian.id) contains _)
    }.verifying("atLeastOneVariant", _.nonEmpty),
    "color" -> stringIn(colorChoices),
    "chat" -> stringIn(chatChoices),
    "ceval" -> stringIn(cevalChoices),
    "percentage" -> text(minLength = 0, maxLength = 3)
      .verifying("invalidTargetPercentage", pct => pct.length == 0 || parseIntOption(pct).??(p => p >= 50 && p <= 100)),
    "fmjd" -> stringIn(fmjdChoices),
    "drawLimit" -> text(minLength = 0, maxLength = 2)
      .verifying("Enter a value between 0 and 99, or leave empty", mvs => mvs.length == 0 || parseIntOption(mvs).??(m => m >= 0 && m <= 99)),
    "noAssistance" -> boolean,
    "team" -> optional(nonEmptyText)
  )(CrudForm.Data.apply)(CrudForm.Data.unapply)) fill empty

  case class Data(
      name: String,
      homepageHours: Int,
      date: DateTime,
      image: String,
      headline: String,
      description: String,
      hostName: String,
      arbiterName: String,
      arbiterHidden: Boolean,
      clockTime: Int,
      clockIncrement: Int,
      clockExtra: Int,
      variants: List[Int],
      color: String,
      chat: String,
      ceval: String,
      percentage: String,
      fmjd: String,
      drawLimit: String,
      noAssistance: Boolean,
      team: Option[String]
  )

  val imageChoices = List(
    "" -> "Lidraughts",
    "chesswhiz.logo.png" -> "ChessWhiz",
    "chessat3.logo.png" -> "Chessat3",
    "bitchess.logo.png" -> "Bitchess"
  )
  val imageDefault = ""

  val cevalChoices = List(
    "disabled" -> "Disabled",
    "arbiter" -> "Arbiter only",
    "accounts" -> "Spectators with account only",
    "spectators" -> "Spectators only",
    "everyone" -> "Everyone"
  )
  val cevalDefault = "disabled"

  val fmjdChoices = List(
    "never" -> "Lidraughts rating",
    "available" -> "FMJD when available",
    "always" -> "FMJD only"
  )
  val fmjdDefault = "never"

  val moderatorClockTimes = (5 to 60 by 5) ++ (70 to 180 by 10)
  val moderatorClockTimeChoices = options(moderatorClockTimes, "%d minute{s}")

  val empty = CrudForm.Data(
    name = "",
    homepageHours = 0,
    date = DateTime.now plusDays 7,
    image = imageDefault,
    headline = "",
    description = "",
    hostName = "",
    arbiterName = "",
    arbiterHidden = false,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockExtra = clockExtraDefault,
    variants = List(draughts.variant.Standard.id),
    color = colorDefault,
    chat = chatDefault,
    ceval = cevalDefault,
    percentage = "",
    fmjd = fmjdDefault,
    drawLimit = "",
    noAssistance = false,
    team = none
  )
}
