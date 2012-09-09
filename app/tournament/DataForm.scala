package lila
package tournament

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

final class DataForm {

  import lila.core.Form._

  val create = Form(mapping(
    "minutes" -> numberIn(Tournament.minuteChoices)
  )(TournamentSetup.apply)(TournamentSetup.unapply)) fill TournamentSetup()
}

case class TournamentSetup(
  minutes: Int = Tournament.minuteDefault)
