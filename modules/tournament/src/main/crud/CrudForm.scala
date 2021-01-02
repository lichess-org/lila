package lila.tournament
package crud

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import chess.StartingPosition
import chess.variant.Variant
import lila.common.Form._

object CrudForm {

  import DataForm._
  import lila.common.Form.UTCDate._

  val maxHomepageHours = 72

  lazy val apply = Form(
    mapping(
      "name"           -> text(minLength = 3, maxLength = 40),
      "homepageHours"  -> number(min = 0, max = maxHomepageHours),
      "clockTime"      -> numberInDouble(clockTimeChoices),
      "clockIncrement" -> numberIn(clockIncrementChoices),
      "clockByoyomi"   -> numberIn(clockByoyomiChoices),
      "periods"        -> numberIn(periodsChoices),
      "minutes"        -> number(min = 20, max = 1440),
      "variant"        -> number.verifying(Variant exists _),
      "position"       -> text.verifying(DataForm.positions contains _),
      "date"           -> utcDate,
      "image"          -> stringIn(imageChoices),
      "headline"       -> text(minLength = 5, maxLength = 30),
      "description"    -> text(minLength = 10, maxLength = 400),
      "conditions"     -> Condition.DataForm.all,
      "berserkable"    -> boolean,
      "teamBattle"     -> boolean
    )(CrudForm.Data.apply)(CrudForm.Data.unapply)
      .verifying("Invalid clock", _.validClock)
      .verifying("Increase tournament duration, or decrease game clock", _.validTiming)
  ) fill CrudForm.Data(
    name = "",
    homepageHours = 0,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockByoyomi = clockByoyomiDefault,
    periods = periodsDefault,
    minutes = minuteDefault,
    variant = chess.variant.Standard.id,
    position = StartingPosition.initial.fen,
    date = DateTime.now plusDays 7,
    image = "",
    headline = "",
    description = "",
    conditions = Condition.DataForm.AllSetup.default,
    berserkable = true,
    teamBattle = false
  )

  case class Data(
      name: String,
      homepageHours: Int,
      clockTime: Double,
      clockIncrement: Int,
      clockByoyomi: Int,
      periods: Int,
      minutes: Int,
      variant: Int,
      position: String,
      date: DateTime,
      image: String,
      headline: String,
      description: String,
      conditions: Condition.DataForm.AllSetup,
      berserkable: Boolean,
      teamBattle: Boolean
  ) {

    def realVariant = Variant orDefault variant

    def validClock = (clockTime + clockIncrement) > 0 || (clockTime + clockByoyomi) > 0

    def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

    private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement + 20 * periods * clockByoyomi
  }

  val imageChoices = List(
    ""                    -> "Lishogi",
    "chesswhiz.logo.png"  -> "ChessWhiz",
    "chessat3.logo.png"   -> "Chessat3",
    "bitchess.logo.png"   -> "Bitchess",
    "offerspill.logo.png" -> "Offerspill"
  )
  val imageDefault = ""
}
