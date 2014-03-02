package lila.app
package templating

import lila.user.User
import lila.tournament.Tournament

import play.api.libs.json.Json

trait TournamentHelper {

  def tournamentJsData(
    tour: Tournament,
    version: Int,
    user: Option[User]) = {

    val data = Json.obj(
      "tournament" -> Json.obj("id" -> tour.id),
      "version" -> version
    )
    Json stringify {
      user.fold(data) { u => data ++ Json.obj("username" -> u.username) }
    }
  }
}
