package lila
package tournament

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

final class DataForm {

  import lila.core.Form._
  import Tournament._

  val create = Form(mapping(
    "clockTime" -> numberIn(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "minutes" -> numberIn(minuteChoices),
    "minPlayers" -> numberIn(minPlayerChoices)
  )(TournamentSetup.apply)(TournamentSetup.unapply)) fill TournamentSetup()
}

case class TournamentSetup(
  clockTime: Int = Tournament.clockTimeDefault,
  clockIncrement: Int = Tournament.clockIncrementDefault,
  minutes: Int = Tournament.minuteDefault,
  minPlayers: Int = Tournament.minPlayerDefault)
