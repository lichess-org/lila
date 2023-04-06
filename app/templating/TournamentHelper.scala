package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.Json

import controllers.routes
import lila.app.ui.ScalatagsTemplate.*
import lila.rating.PerfType
import lila.tournament.{ Schedule, Tournament }
import lila.user.User
import lila.common.Json.given

trait TournamentHelper extends HasEnv:
  self: I18nHelper with DateHelper with UserHelper with StringHelper with NumberHelper =>

  def netBaseUrl: String

  def tournamentJsData(tour: Tournament, version: Int, user: Option[User]) =

    val data = Json.obj(
      "tournament" -> Json.obj("id" -> tour.id),
      "version"    -> version
    )
    Json stringify {
      user.fold(data) { u =>
        data ++ Json.obj("username" -> u.username)
      }
    }

  def tournamentLink(tour: Tournament)(using Lang): Frag =
    a(
      dataIcon := "",
      cls      := (if (tour.isScheduled) "text is-gold" else "text"),
      href     := routes.Tournament.show(tour.id.value).url
    )(tour.name())

  def tournamentLink(tourId: TourId)(using Lang): Frag =
    a(
      dataIcon := "",
      cls      := "text",
      href     := routes.Tournament.show(tourId.value).url
    )(tournamentIdToName(tourId))

  def tournamentIdToName(id: TourId)(using Lang): String =
    env.tournament.getTourName sync id getOrElse "Tournament"

  object scheduledTournamentNameShortHtml:
    private def icon(c: Char) = s"""<span data-icon="$c"></span>"""
    private val replacements = List(
      "Lichess "    -> "",
      "Marathon"    -> icon(''),
      "HyperBullet" -> s"H${icon(PerfType.Bullet.iconChar)}",
      "SuperBlitz"  -> s"S${icon(PerfType.Blitz.iconChar)}"
    ) ::: PerfType.leaderboardable.filterNot(PerfType.translated.contains).map { pt =>
      pt.trans(using lila.i18n.defaultLang) -> icon(pt.iconChar)
    }

    def apply(name: String): Frag =
      raw {
        replacements.foldLeft(name) { case (n, (from, to)) =>
          n.replace(from, to)
        }
      }

  def tournamentIconChar(tour: Tournament): String =
    tour.schedule.map(_.freq) match
      case Some(Schedule.Freq.Marathon | Schedule.Freq.ExperimentalMarathon) => ""
      case _ => tour.spotlight.flatMap(_.iconFont) | tour.perfType.iconChar.toString
