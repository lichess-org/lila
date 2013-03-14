package lila.app
package tournament

import http.Context
import user.User

import play.api.libs.json.Json
import scala.math.{ min, max, round }

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
      user.fold(data) { u ⇒ data ++ Json.obj("username" -> u.username) }
    }
  }
}
