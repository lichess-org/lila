package lila.tournament
package ui

import play.api.i18n.Lang

import lila.core.i18n.Translate
import lila.rating.PerfType
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.ClientName

final class TournamentUi(helpers: Helpers)(getTourName: GetTourName):
  import helpers.{ *, given }

  object finishedList:

    def apply(finished: List[Tournament])(using Context): Tag =
      tbody(finished.map(apply))

    def apply(t: Tournament)(using Context): Tag =
      tr(cls := "paginated")(
        td(cls := "icon")(iconEl(tournamentIcon(t))),
        header(t),
        td(cls := "date")(momentFromNow(t.startsAt)),
        td(cls := "players")(
          span(
            iconEl(Icon.Trophy)(cls := "text"),
            userIdLink(t.winnerId, withOnline = false)
          ),
          span(trans.site.nbPlayers.plural(t.nbPlayers, t.nbPlayers.localize))
        )
      )

    def header(t: Tournament)(using Context) =
      td(cls := "header")(
        a(href := routes.Tournament.show(t.id))(
          span(cls := "name")(t.name()),
          span(
            t.clock.show,
            " • ",
            if t.variant.exotic then t.variant.name else t.perfType.trans,
            t.position.isDefined.option(frag(" • ", trans.site.thematic())),
            " • ",
            lila.gathering.ui.translateRated(t.rated),
            " • ",
            t.durationString
          )
        )
      )

  def notFound(using Context) =
    Page(trans.site.tournamentNotFound.txt()):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.tournamentNotFound()),
        p(trans.site.tournamentDoesNotExist()),
        p(trans.site.tournamentMayHaveBeenCanceled()),
        br,
        br,
        a(href := routes.Tournament.home)(trans.site.returnToTournamentsHomepage())
      )

  def enterable(tours: List[Tournament])(using Context) =
    table(cls := "tournaments")(
      tours.map: tour =>
        val visiblePlayers = (tour.nbPlayers >= 10).option(tour.nbPlayers)
        val timeTag =
          if tour.isStarted then timeRemaining(tour.finishesAt)
          else momentFromNow(tour.startsAt)
        tr(
          td(
            a(cls := "text", href := routes.Tournament.show(tour.id))(
              iconEl(tournamentIcon(tour)),
              tour.name(full = false)
            )
          ),
          td(cls := "progress-td")(
            span(cls := "progress")(
              timeTag(cls := "progress__text"),
              span(cls := "progress__bar", st.style := s"width:${tour.progressPercent}%")
            )
          ),
          td(tour.durationString),
          tour.conditions.teamMember match
            case Some(t) =>
              td(iconEl := Icon.Group, cls := "text tour-team-icon", title := t.teamName)(
                visiblePlayers
              )
            case _ if tour.isTeamBattle =>
              td(
                iconEl := Icon.Group,
                cls := "text tour-team-icon",
                title := trans.team.teamBattle.txt()
              ):
                visiblePlayers
            case None => td(iconEl := Icon.User, cls := "text")(visiblePlayers)
        )
    )

  def tournamentLink(tour: Tournament)(using Translate): Tag =
    a(
      iconEl := Icon.Trophy,
      cls := (if tour.isScheduled then "text is-gold" else "text"),
      href := routes.Tournament.show(tour.id).url
    )(tour.name())

  def tournamentLink(tourId: TourId)(using Translate, ClientName): Tag =
    a(
      iconEl := Icon.Trophy,
      cls := "text",
      href := routes.Tournament.show(tourId).url
    )(tournamentIdToName(tourId))

  def tournamentIdToName(id: TourId)(using Lang)(using client: ClientName): String =
    client.isHuman.so(getTourName.sync(id)).getOrElse(s"Tournament #$id")

  def teamTournamentRow(t: Tournament)(using Translate) =
    tr(cls := List("enterable" -> t.isEnterable, "soon" -> t.isNowOrSoon))(
      td(cls := "icon")(iconEl(tournamentIcon(t))),
      td(cls := "header")(
        a(href := routes.Tournament.show(t.id))(
          span(cls := "name")(t.name()),
          span(cls := "setup")(
            t.clock.show,
            " • ",
            if t.variant.exotic then t.variant.name else t.perfType.trans,
            t.position.isDefined.option(frag(" • ", trans.site.thematic())),
            " • ",
            lila.gathering.ui.translateRated(t.rated),
            " • ",
            t.durationString
          )
        )
      ),
      td(cls := "infos")(
        t.teamBattle.fold(trans.team.innerTeam()): battle =>
          trans.team.battleOfNbTeams.plural(battle.teams.size, battle.teams.size.localize),
        br,
        if t.isEnterable && t.startsAt.isBeforeNow then trans.site.eventInProgress()
        else momentFromNowOnce(t.startsAt)
      ),
      td(cls := "text", iconEl := Icon.User)(t.nbPlayers.localize)
    )

  object scheduledTournamentNameShortHtml:
    private def icon(c: Icon) = iconEl(c).render
    private val replacements =
      given lila.core.i18n.Translate = transDefault
      List(
        "Lichess " -> "",
        "Marathon" -> icon(Icon.Globe),
        "HyperBullet" -> s"H${icon(PerfType.Bullet.icon)}",
        "SuperBlitz" -> s"S${icon(PerfType.Blitz.icon)}"
      ) ::: lila.rating.PerfType.leaderboardable
        .filterNot(lila.rating.PerfType.translated.contains)
        .map(PerfType(_))
        .map: pt =>
          pt.trans -> icon(pt.icon)
    def apply(name: String): Frag = raw:
      replacements.foldLeft(name):
        case (n, (from, to)) => n.replace(from, to)

  def tournamentIcon(tour: Tournament): Icon =
    if tour.isMarathon then Icon.Globe
    else tour.spotlight.flatMap(_.icon) | tour.perfType.icon
