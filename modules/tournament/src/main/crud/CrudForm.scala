package lila.tournament
package crud

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import chess.variant.Variant
import lila.common.Form._
import chess.format.FEN

final class CrudForm(repo: TournamentRepo) {

  import CrudForm._
  import TournamentForm._
  import lila.common.Form.UTCDate._

  def apply(tour: Option[Tournament]) = Form(
    mapping(
      "id"             -> id(8, tour.map(_.id))(repo.exists),
      "name"           -> text(minLength = 3, maxLength = 40),
      "homepageHours"  -> number(min = 0, max = maxHomepageHours),
      "clockTime"      -> numberInDouble(clockTimeChoices),
      "clockIncrement" -> numberIn(clockIncrementChoices),
      "minutes"        -> number(min = 20, max = 1440),
      "variant"        -> number.verifying(Variant exists _),
      "position"       -> optional(lila.common.Form.fen.playableStrict),
      "date"           -> utcDate,
      "image"          -> stringIn(imageChoices),
      "headline"       -> text(minLength = 5, maxLength = 30),
      "description"    -> nonEmptyText,
      "conditions"     -> Condition.DataForm.all(Nil),
      "berserkable"    -> boolean,
      "streakable"     -> boolean,
      "teamBattle"     -> boolean,
      "hasChat"        -> boolean
    )(Data.apply)(Data.unapply)
      .verifying("Invalid clock", _.validClock)
      .verifying("Increase tournament duration, or decrease game clock", _.validTiming)
  ) fill Data(
    id = Tournament.makeId,
    name = "",
    homepageHours = 0,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    variant = chess.variant.Standard.id,
    position = none,
    date = DateTime.now plusDays 7,
    image = "",
    headline = "",
    description = "",
    conditions = Condition.DataForm.AllSetup.default,
    berserkable = true,
    streakable = true,
    teamBattle = false,
    hasChat = true
  )
}

object CrudForm {

  val maxHomepageHours = 24

  case class Data(
      id: String,
      name: String,
      homepageHours: Int,
      clockTime: Double,
      clockIncrement: Int,
      minutes: Int,
      variant: Int,
      position: Option[FEN],
      date: DateTime,
      image: String,
      headline: String,
      description: String,
      conditions: Condition.DataForm.AllSetup,
      berserkable: Boolean,
      streakable: Boolean,
      teamBattle: Boolean,
      hasChat: Boolean
  ) {

    def realVariant = Variant orDefault variant

    def realPosition = position ifTrue realVariant.standard

    def validClock = (clockTime + clockIncrement) > 0

    def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

    private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement
  }

  val imageChoices = List(
    ""                    -> "Lichess",
    "offerspill.logo.png" -> "Offerspill"
  )
  val imageDefault = ""
}
