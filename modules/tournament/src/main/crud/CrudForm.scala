package lila.tournament
package crud

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import shogi.variant.Variant
import lila.common.Form._
import shogi.format.forsyth.Sfen

object CrudForm {

  import lila.common.Form.UTCDate._

  val maxHomepageHours = 24 * 7

  lazy val apply = Form(
    mapping(
      "name"             -> text(minLength = 3, maxLength = 40),
      "homepageHours"    -> number(min = 0, max = maxHomepageHours),
      "timeControlSetup" -> TimeControl.DataForm.setup,
      "minutes"          -> number(min = 20, max = 1440),
      "variant"          -> number.verifying(Variant exists _),
      "position"         -> optional(lila.common.Form.sfen.clean),
      "date"             -> utcDate,
      "image"            -> stringIn(imageChoices),
      "headline"         -> text(minLength = 5, maxLength = 30),
      "description"      -> text(minLength = 10, maxLength = 400),
      "conditions"       -> Condition.DataForm.all,
      "berserkable"      -> boolean,
      "teamBattle"       -> boolean
    )(CrudForm.Data.apply)(CrudForm.Data.unapply)
      .verifying("Invalid clock", _.validClock)
      .verifying("Increase tournament duration, or decrease game clock", _.validTiming)
      .verifying("Custom position is not valid", _.isCustomPositionValid)
  ) fill CrudForm.Data(
    name = "",
    homepageHours = 0,
    timeControlSetup = TimeControl.DataForm.Setup.default,
    minutes = DataForm.minuteDefault,
    variant = shogi.variant.Standard.id,
    position = none,
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
      timeControlSetup: TimeControl.DataForm.Setup,
      minutes: Int,
      variant: Int,
      position: Option[Sfen],
      date: DateTime,
      image: String,
      headline: String,
      description: String,
      conditions: Condition.DataForm.AllSetup,
      berserkable: Boolean,
      teamBattle: Boolean
  ) {

    def realVariant = Variant orDefault variant

    def validClock =
      if (timeControlSetup.isCorrespondence)
        timeControlSetup.daysPerTurn > 0
      else
        (timeControlSetup.clockTime + timeControlSetup.clockIncrement) > 0 || (timeControlSetup.clockTime + timeControlSetup.clockByoyomi) > 0

    def isCustomPositionValid =
      position.fold(true) { sfen =>
        sfen.toSituation(realVariant).exists(_.playable(strict = true, withImpasse = true))
      }

    def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

    private def estimatedGameDuration =
      60 * timeControlSetup.clockTime + 30 * timeControlSetup.clockIncrement + 20 * timeControlSetup.periods * timeControlSetup.clockByoyomi
  }

  val imageChoices = List(
    ""                  -> "Lishogi",
    "bougyoku.logo.png" -> "Bougyoku"
  )
  val imageDefault = ""
}
