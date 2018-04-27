package lila.tournament
package crud

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import chess.StartingPosition
import lila.common.Form._

object CrudForm {

  import DataForm._
  import lila.common.Form.UTCDate._

  val maxHomepageHours = 72

  lazy val apply = Form(mapping(
    "name" -> nonEmptyText(minLength = 3, maxLength = 40),
    "homepageHours" -> number(min = 0, max = maxHomepageHours),
    "clockTime" -> numberInDouble(clockTimePrivateChoices),
    "clockIncrement" -> numberIn(clockIncrementPrivateChoices),
    "minutes" -> number(min = 20, max = 1440),
    "variant" -> number.verifying(validVariantIds contains _),
    "position" -> nonEmptyText.verifying(DataForm.positions contains _),
    "date" -> utcDate,
    "image" -> stringIn(imageChoices),
    "headline" -> nonEmptyText(minLength = 5, maxLength = 30),
    "description" -> nonEmptyText(minLength = 10, maxLength = 400),
    "conditions" -> Condition.DataForm.all,
    "berserkable" -> boolean
  )(CrudForm.Data.apply)(CrudForm.Data.unapply)
    .verifying("Invalid clock", _.validClock)
    .verifying("Increase tournament duration, or decrease game clock", _.validTiming)) fill CrudForm.Data(
    name = "",
    homepageHours = 0,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    variant = chess.variant.Standard.id,
    position = StartingPosition.initial.fen,
    date = DateTime.now plusDays 7,
    image = "",
    headline = "",
    description = "",
    conditions = Condition.DataForm.AllSetup.default,
    berserkable = true
  )

  case class Data(
      name: String,
      homepageHours: Int,
      clockTime: Double,
      clockIncrement: Int,
      minutes: Int,
      variant: Int,
      position: String,
      date: DateTime,
      image: String,
      headline: String,
      description: String,
      conditions: Condition.DataForm.AllSetup,
      berserkable: Boolean
  ) {

    def validClock = (clockTime + clockIncrement) > 0

    def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

    private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement
  }

  val imageChoices = List(
    "" -> "Lichess",
    "chesswhiz.logo.png" -> "ChessWhiz",
    "chessat3.logo.png" -> "Chessat3",
    "bitchess.logo.png" -> "Bitchess"
  )
  val imageDefault = ""
}
