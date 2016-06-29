package lila.app
package templating

import controllers.routes
import lila.api.Context
import lila.tournament.Env.{ current => tournamentEnv }
import lila.tournament.{ Tournament, TournamentRepo, System, Schedule }
import lila.user.{ User, UserContext }

import org.joda.time.DateTime

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
    val cssClass = if (tour.scheduled) "text is-gold" else "text"
    if (tour.`private` && DateTime.now.compareTo(tour.finishesAt) == -1) {
      s"""<span class="text" data-icon="g">${tour.fullName}</span>"""
    } else {
      val url = routes.Tournament.show(tour.id)
      s"""<a class="text" data-icon="g" class="$cssClass" href="$url">${tour.fullName}</a>"""
    }
  }

  def tournamentLink(tourId: String): Html = Html {
    val tour = tournamentIdToTournament(tourId)
    if (tour == null) {
      var url = routes.Tournament.show(tour.id)
      s"""<a class="text" data-icon="g" href="$url">Tournament</a>"""
    }
    val tourName = tour.fullName
    if (tour.`private` && DateTime.now.compareTo(tour.finishesAt) == -1) {
      s"""<span class="text" data-icon="g">$tourName</span>"""
    } else {
      val url = routes.Tournament.show(tourId)
      s"""<a class="text" data-icon="g" href="$url">$tourName</a>"""
    }
  }

  def tournamentIdToTournament(id: String) = tournamentEnv.cached byId id getOrElse null

  object scheduledTournamentNameShortHtml {
    private def icon(c: Char) = s"""<span data-icon="$c"></span>"""
    private val replacements = List(
      "Lichess " -> "",
      "Marathon" -> icon('\\'),
      "SuperBlitz" -> icon(lila.rating.PerfType.Blitz.iconChar)
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

  private def longTournamentDescription(tour: Tournament) =
    s"${tour.nbPlayers} players compete in the ${showEnglishDate(tour.startsAt)} ${tour.fullName}. " +
      s"${tour.clock.show} ${tour.mode.name} games are played during ${tour.minutes} minutes. " +
      tour.winnerId.fold("Winner is not yet decided.") { winnerId =>
        s"${usernameOrId(winnerId)} takes the prize home!"
      }

  def tournamentOpenGraph(tour: Tournament) = lila.app.ui.OpenGraph(
    title = s"${tour.fullName}: ${tour.variant.name} ${tour.clock.show} ${tour.mode.name} #${tour.id}",
    url = s"$netBaseUrl${routes.Tournament.show(tour.id).url}",
    description = longTournamentDescription(tour))
}
