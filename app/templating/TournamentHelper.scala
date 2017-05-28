package lila.app
package templating

import controllers.routes
import lila.tournament.Env.{ current => tournamentEnv }
import lila.tournament.{ Tournament, System, Schedule }
import lila.user.{ User, UserContext }

import play.api.libs.json.Json
import play.twirl.api.Html

trait TournamentHelper { self: I18nHelper with DateHelper with UserHelper =>

  def netBaseUrl: String

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
    val cssClass = if (tour.isScheduled) "text is-gold" else "text"
    val url = routes.Tournament.show(tour.id)
    s"""<a data-icon="g" class="$cssClass" href="$url">${tour.fullName}</a>"""
  }

  def tournamentLink(tourId: String): Html = Html {
    val url = routes.Tournament.show(tourId)
    s"""<a class="text" data-icon="g" href="$url">${tournamentIdToName(tourId)}</a>"""
  }

  def tournamentIdToName(id: String) = tournamentEnv.cached name id getOrElse "Tournament"

  object scheduledTournamentNameShortHtml {
    private def icon(c: Char) = s"""<span data-icon="$c"></span>"""
    private val replacements = List(
      "Lichess " -> "",
      "Marathon" -> icon('\\'),
      "HyperBullet" -> s"H${icon(lila.rating.PerfType.Bullet.iconChar)}",
      "SuperBlitz" -> s"S${icon(lila.rating.PerfType.Blitz.iconChar)}"
    ) ::: lila.rating.PerfType.leaderboardable.map { pt =>
        pt.name -> icon(pt.iconChar)
      }
    def apply(name: String) = Html {
      replacements.foldLeft(name) {
        case (n, (from, to)) => n.replace(from, to)
      }
    }
  }

  def systemName(sys: System)(implicit ctx: UserContext) = sys match {
    case System.Arena => System.Arena.toString
  }

  def tournamentIconChar(tour: Tournament): Char = tour.schedule.map(_.freq) match {
    case Some(Schedule.Freq.Marathon | Schedule.Freq.ExperimentalMarathon) => '\\'
    case _ => tour.perfType.fold('g')(_.iconChar)
  }
}
