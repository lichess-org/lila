package lila.tournament
package ui

import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.mvc.Call

import lila.ui.ScalatagsTemplate.*
import lila.common.Json.given
import lila.rating.PerfType
import lila.core.i18n.Translate
import lila.ui.Icon
import lila.core.user.User

final class TournamentHelper(
    routeTournamentShow: String => Call,
    getTourName: GetTourName,
    defaultTranslate: Translate
):
  def tournamentJsData(tour: Tournament, version: Int, user: Option[User]) =
    val data = Json.obj(
      "tournament" -> Json.obj("id" -> tour.id),
      "version"    -> version
    )
    Json.stringify:
      user.fold(data): u =>
        data ++ Json.obj("username" -> u.username)

  def tournamentLink(tour: Tournament)(using Translate): Frag =
    a(
      dataIcon := Icon.Trophy.value,
      cls      := (if tour.isScheduled then "text is-gold" else "text"),
      href     := routeTournamentShow(tour.id.value).url
    )(tour.name())

  def tournamentLink(tourId: TourId)(using Translate): Frag =
    a(
      dataIcon := Icon.Trophy.value,
      cls      := "text",
      href     := routeTournamentShow(tourId.value).url
    )(tournamentIdToName(tourId))

  def tournamentIdToName(id: TourId)(using translate: Translate): String =
    getTourName.sync(id)(using translate.lang).getOrElse("Tournament")

  object scheduledTournamentNameShortHtml:
    private def icon(c: Icon) = s"""<span data-icon="$c"></span>"""
    private val replacements =
      given lila.core.i18n.Translate = defaultTranslate
      List(
        "Lichess "    -> "",
        "Marathon"    -> icon(Icon.Globe),
        "HyperBullet" -> s"H${icon(PerfType.Bullet.icon)}",
        "SuperBlitz"  -> s"S${icon(PerfType.Blitz.icon)}"
      ) ::: lila.rating.PerfType.leaderboardable
        .filterNot(lila.rating.PerfType.translated.contains)
        .map(PerfType(_))
        .map: pt =>
          pt.trans -> icon(pt.icon)
    def apply(name: String): Frag = raw:
      replacements.foldLeft(name):
        case (n, (from, to)) => n.replace(from, to)

  def tournamentIcon(tour: Tournament): Icon =
    tour.schedule.map(_.freq) match
      case Some(Schedule.Freq.Marathon | Schedule.Freq.ExperimentalMarathon) => Icon.Globe
      case _ => tour.spotlight.flatMap(_.iconFont) | tour.perfType.icon
