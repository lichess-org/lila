package lila.tournament
package crud

import play.api.data.*
import play.api.data.Forms.*

import chess.variant.Variant
import chess.format.Fen
import chess.Clock.IncrementSeconds
import lila.common.Form.{ given, * }
import lila.gathering.GatheringClock

final class CrudForm(repo: TournamentRepo):

  import CrudForm.*
  import TournamentForm.*
  import GatheringClock.*

  def apply(tour: Option[Tournament]) = Form(
    mapping(
      "id"             -> id[TourId](8, tour.map(_.id))(repo.exists),
      "name"           -> text(minLength = 3, maxLength = 40),
      "homepageHours"  -> number(min = 0, max = maxHomepageHours),
      "clockTime"      -> numberInDouble(timeChoices),
      "clockIncrement" -> numberIn(incrementChoices).into[IncrementSeconds],
      "minutes"        -> number(min = 20, max = 1440),
      "variant"        -> typeIn(Variant.list.all.map(_.id).toSet),
      "position"       -> optional(lila.common.Form.fen.playableStrict),
      "date"           -> PrettyDateTime.mapping,
      "image"          -> stringIn(imageChoices),
      "headline"       -> text(minLength = 5, maxLength = 30),
      "description"    -> nonEmptyText,
      "conditions"     -> TournamentCondition.form.all(Nil),
      "rated"          -> boolean,
      "berserkable"    -> boolean,
      "streakable"     -> boolean,
      "teamBattle"     -> boolean,
      "hasChat"        -> boolean
    )(Data.apply)(unapply)
      .verifying("Invalid clock", _.validClock)
      .verifying("Increase tournament duration, or decrease game clock", _.validTiming)
  ) fill Data(
    id = Tournament.makeId,
    name = "",
    homepageHours = 0,
    clockTime = timeDefault,
    clockIncrement = incrementDefault,
    minutes = minuteDefault,
    variant = chess.variant.Standard.id,
    position = none,
    date = nowDateTime plusDays 7,
    image = "",
    headline = "",
    description = "",
    conditions = TournamentCondition.All.empty,
    berserkable = true,
    rated = true,
    streakable = true,
    teamBattle = false,
    hasChat = true
  )

object CrudForm:

  val maxHomepageHours = 24

  case class Data(
      id: TourId,
      name: String,
      homepageHours: Int,
      clockTime: Double,
      clockIncrement: IncrementSeconds,
      minutes: Int,
      variant: Variant.Id,
      position: Option[Fen.Epd],
      date: LocalDateTime,
      image: String,
      headline: String,
      description: String,
      conditions: TournamentCondition.All,
      rated: Boolean,
      berserkable: Boolean,
      streakable: Boolean,
      teamBattle: Boolean,
      hasChat: Boolean
  ):

    def realVariant = Variant orDefault variant

    def realPosition: Option[Fen.Opening] = position.ifTrue(realVariant.standard).map(_.opening)

    def validClock = (clockTime + clockIncrement.value) > 0

    def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

    private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement.value

  val imageChoices = List(
    ""                    -> "Lichess",
    "offerspill.logo.png" -> "Offerspill"
  )
  val imageDefault = ""
