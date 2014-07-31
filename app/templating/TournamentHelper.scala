package lila.app
package templating

import controllers.routes
import lila.api.Context
import lila.tournament.{ Tournament, System }
import lila.user.{ User, UserContext }

import play.api.libs.json.Json
import play.twirl.api.Html

trait TournamentHelper { self: I18nHelper =>

  def tournamentJsData(tour: Tournament, version: Int, user: Option[User]) = {

    val data = Json.obj(
      "tournament" -> Json.obj("id" -> tour.id),
      "version" -> version
    )
    Json stringify {
      user.fold(data) { u => data ++ Json.obj("username" -> u.username) }
    }
  }

  def tournamentLink(tour: Tournament) = Html {
    val cssClass = if (tour.scheduled) "is-gold" else ""
    val url = routes.Tournament.show(tour.id)
    s"""<a data-icon="g" class="$cssClass" href="$url">&nbsp;${tour.fullName}</a>"""
  }

  def systemName(sys: System)(implicit ctx: UserContext) = sys match {
    case System.Arena  => System.Arena.toString
    case System.Swiss  => System.Swiss.toString
  }
}
