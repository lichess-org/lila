package lila
package tournament

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

final class DataForm {

  val create = Form(mapping(
    "maxUsers" -> number.verifying(min(3), max(8))
  )(TournamentSetup.apply)(TournamentSetup.unapply))
}

case class TournamentSetup(
  maxUsers: Int)
