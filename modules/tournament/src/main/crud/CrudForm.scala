package lila.tournament
package crud

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._

import shogi.variant.Variant
import lila.common.Form._
import shogi.format.forsyth.Sfen

object CrudForm {

  import DataForm._
  import lila.common.Form.UTCDate._

  val maxHomepageHours = 24 * 7

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
      "position"       -> optional(lila.common.Form.sfen.clean),
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
      .verifying("Custom position is not valid", _.isCustomPositionValid)
  ) fill CrudForm.Data(
    name = "",
    homepageHours = 0,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    clockByoyomi = clockByoyomiDefault,
    periods = periodsDefault,
    minutes = minuteDefault,
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
      clockTime: Double,
      clockIncrement: Int,
      clockByoyomi: Int,
      periods: Int,
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

    def validClock = (clockTime + clockIncrement) > 0 || (clockTime + clockByoyomi) > 0

    def isCustomPositionValid =
      position.fold(true) { sfen =>
        sfen.toSituation(realVariant).exists(_.playable(strict = true, withImpasse = true))
      }

    def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

    private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement + 20 * periods * clockByoyomi
  }

  val imageChoices = List(
    ""                  -> "Lishogi",
    "bougyoku.logo.png" -> "Bougyoku"
  )
  val imageDefault = ""
}
