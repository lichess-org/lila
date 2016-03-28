package lila.tournament
package crud

import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import scala.util.Try

import lila.common.Form._

object CrudForm {

  import DataForm._

  lazy val apply = Form(mapping(
    "name" -> nonEmptyText(minLength = 3, maxLength = 40),
    "homepageHours" -> number(min = 0, max = 24),
    "clockTime" -> numberInDouble(clockTimePrivateChoices),
    "clockIncrement" -> numberIn(clockIncrementPrivateChoices),
    "minutes" -> numberIn(minutePrivateChoices),
    "variant" -> number.verifying(validVariantIds contains _),
    "date" -> nonEmptyText.verifying(s => parseDateUTC(s).isDefined),
    "dateHour" -> number(min = 0, max = 23),
    "dateMinute" -> number(min = 0, max = 59),
    "image" -> stringIn(imageChoices),
    "headline" -> nonEmptyText(minLength = 5, maxLength = 30),
    "description" -> nonEmptyText(minLength = 10, maxLength = 400)
  )(CrudForm.Data.apply)(CrudForm.Data.unapply)
    .verifying("Invalid clock", _.validClock)
    .verifying("Increase tournament duration, or decrease game clock", _.validTiming)
  ) fill CrudForm.Data(
    name = "",
    homepageHours = 0,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    variant = chess.variant.Standard.id,
    date = dateFormatter.print(DateTime.now),
    dateHour = 0,
    dateMinute = 0,
    image = "",
    headline = "",
    description = "")

  case class Data(
      name: String,
      homepageHours: Int,
      clockTime: Double,
      clockIncrement: Int,
      minutes: Int,
      variant: Int,
      date: String,
      dateHour: Int,
      dateMinute: Int,
      image: String,
      headline: String,
      description: String) {

    def actualDate = parseDateUTC(date).err(s"Invalid date $date")
      .withHourOfDay(dateHour)
      .withMinuteOfHour(dateMinute)

    def validClock = (clockTime + clockIncrement) > 0

    def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

    private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement
  }

  val imageChoices = List(
    "" -> "Lichess",
    "chesswhiz.logo.png" -> "ChessWhiz",
    "chessat3.logo.png" -> "Chessat3")
  val imageDefault = ""

  val dateFormatter = DateTimeFormat forPattern "yyyy-MM-dd" withZone DateTimeZone.UTC

  private def parseDateUTC(str: String): Option[DateTime] =
    Try(dateFormatter parseDateTime str).toOption
}
