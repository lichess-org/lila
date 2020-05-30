package lidraughts.tournament
package crud

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import draughts.StartingPosition
import draughts.variant.Variant
import lidraughts.common.Form._

object CrudForm {

  import DataForm._
  import lidraughts.common.Form.UTCDate._

  val maxHomepageHours = 72

  lazy val apply = Form(mapping(
    "name" -> text(minLength = 3, maxLength = 40),
    "homepageHours" -> number(min = 0, max = maxHomepageHours),
    "clockTime" -> numberInDouble(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "minutes" -> number(min = 20, max = 1440),
    "variant" -> number.verifying(Variant exists _),
    "position_standard" -> optional(nonEmptyText),
    "position_russian" -> optional(nonEmptyText),
    "date" -> utcDate,
    "image" -> stringIn(imageChoices),
    "headline" -> text(minLength = 5, maxLength = 30),
    "description" -> text(minLength = 10, maxLength = 800),
    "conditions" -> Condition.DataForm.all,
    "password" -> optional(nonEmptyText),
    "berserkable" -> boolean
  )(CrudForm.Data.apply)(CrudForm.Data.unapply)
    .verifying("Invalid clock", _.validClock)
    .verifying("Increase tournament duration, or decrease game clock", _.validTiming)) fill CrudForm.Data(
    name = "",
    homepageHours = 0,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    variant = draughts.variant.Standard.id,
    positionStandard = draughts.variant.Standard.initialFen.some,
    positionRussian = draughts.variant.Russian.initialFen.some,
    date = DateTime.now plusDays 7,
    image = "",
    headline = "",
    description = "",
    conditions = Condition.DataForm.AllSetup.default,
    password = None,
    berserkable = true
  )

  case class Data(
      name: String,
      homepageHours: Int,
      clockTime: Double,
      clockIncrement: Int,
      minutes: Int,
      variant: Int,
      positionStandard: Option[String],
      positionRussian: Option[String],
      date: DateTime,
      image: String,
      headline: String,
      description: String,
      conditions: Condition.DataForm.AllSetup,
      password: Option[String],
      berserkable: Boolean
  ) {

    def realVariant = Variant orDefault variant

    def validClock = (clockTime + clockIncrement) > 0

    def validTiming = password.isDefined || (minutes * 60) >= (3 * estimatedGameDuration)

    private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement
  }

  val imageChoices = List(
    "" -> "Lidraughts",
    "chesswhiz.logo.png" -> "ChessWhiz",
    "chessat3.logo.png" -> "Chessat3",
    "bitchess.logo.png" -> "Bitchess"
  )
  val imageDefault = ""
}
