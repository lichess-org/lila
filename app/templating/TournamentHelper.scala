package lila.app
package templating

import controllers.routes
import lila.api.Context
import lila.tournament.Env.{ current => tournamentEnv }
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

  def tournamentLink(tour: Tournament): Html = Html {
    val cssClass = if (tour.scheduled) "text is-gold" else "text"
    val url = routes.Tournament.show(tour.id)
    s"""<a data-icon="g" class="$cssClass" href="$url">${tour.fullName}</a>"""
  }

  def tournamentLink(tourId: String): Html = Html {
    val url = routes.Tournament.show(tourId)
    s"""<a class="text" data-icon="g" href="$url">${tournamentIdToName(tourId)}</a>"""
  }

  def tournamentIdToName(id: String) = tournamentEnv.cached name id getOrElse "Tournament"

  object scheduledTournamentNameShortHtml {
    import lila.rating.PerfType._
    private def icon(c: Char) = s"""<span data-icon="$c"></span>"""
    private val replacements = List(
      "Lichess " -> "",
      "Bullet" -> icon(Bullet.iconChar),
      "Blitz" -> icon(Blitz.iconChar),
      "SuperBlitz" -> icon(Blitz.iconChar),
      "Classical" -> icon(Classical.iconChar)
    )
    def apply(name: String) = Html {
      replacements.foldLeft(name) {
        case (n, (from, to)) => n.replace(from, to)
      }
    }
  }

  def systemName(sys: System)(implicit ctx: UserContext) = sys match {
    case System.Arena => System.Arena.toString
    case System.Swiss => System.Swiss.toString
  }

  def tournamentIconChar(tour: Tournament): Char = tour.schedule.map(_.freq) match {
    case Some(lila.tournament.Schedule.Freq.Marathon) => '\\'
    case _ => tour.perfType.fold('g')(_.iconChar)
  }
}
