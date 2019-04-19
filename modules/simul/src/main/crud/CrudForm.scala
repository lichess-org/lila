package lidraughts.simul
package crud

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import draughts.StartingPosition
import lidraughts.common.Form._

object CrudForm {

  import DataForm._
  import lidraughts.common.Form.UTCDate._

  val maxHomepageHours = 336

  lazy val apply = Form(mapping(
    "name" -> nonEmptyText(minLength = 3, maxLength = 40),
    "homepageHours" -> number(min = 0, max = maxHomepageHours),
    "date" -> utcDate,
    "image" -> stringIn(imageChoices),
    "headline" -> nonEmptyText(minLength = 5, maxLength = 30),
    "description" -> nonEmptyText(minLength = 10, maxLength = 400),
    "hostName" -> nonEmptyText(minLength = 2, maxLength = 20),
    "arbiterName" -> text(minLength = 0, maxLength = 20),
    "clockTime" -> numberIn(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "clockExtra" -> numberIn(clockExtraChoices),
    "variants" -> list {
      number.verifying(Set(draughts.variant.Standard.id, draughts.variant.Frisian.id, draughts.variant.Frysk.id, draughts.variant.Antidraughts.id, draughts.variant.Breakthrough.id) contains _)
    }.verifying("atLeastOneVariant", _.nonEmpty),
    "color" -> stringIn(colorChoices),
    "chat" -> stringIn(chatChoices),
    "percentage" -> text(minLength = 0, maxLength = 3)
      .verifying("invalidTargetPercentage", pct => pct.length == 0 || parseIntOption(pct).fold(false)(p => p >= 50 && p <= 100))
  )(CrudForm.Data.apply)(CrudForm.Data.unapply)) fill empty

  val empty = CrudForm.Data(
    name = "",
    homepageHours = 0,
    date = DateTime.now plusDays 7,
    image = "",
    headline = "",
    description = "",
    hostName = "",
    arbiterName = "",
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockExtra = clockExtraDefault,
    variants = List(draughts.variant.Standard.id),
    color = colorDefault,
    chat = chatDefault,
    percentage = ""
  )

  case class Data(
      name: String,
      homepageHours: Int,
      date: DateTime,
      image: String,
      headline: String,
      description: String,
      hostName: String,
      arbiterName: String,
      clockTime: Int,
      clockIncrement: Int,
      clockExtra: Int,
      variants: List[Int],
      color: String,
      chat: String,
      percentage: String
  )

  val imageChoices = List(
    "" -> "Lidraughts",
    "chesswhiz.logo.png" -> "ChessWhiz",
    "chessat3.logo.png" -> "Chessat3",
    "bitchess.logo.png" -> "Bitchess"
  )
  val imageDefault = ""
}
