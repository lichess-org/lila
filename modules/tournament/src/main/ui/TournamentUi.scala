package lila.tournament
package ui

import lila.core.i18n.Translate
import lila.rating.PerfType
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TournamentUi(helpers: Helpers)(getTourName: GetTourName):
  import helpers.{ *, given }

  object finishedList:

    def apply(finished: List[Tournament])(using Context): Tag =
      tbody(finished.map(apply))

    def apply(t: Tournament)(using Context): Tag =
      tr(cls := "paginated")(
        td(cls := "icon")(iconTag(tournamentIcon(t))),
        header(t),
        td(cls := "date")(momentFromNow(t.startsAt)),
        td(cls := "players")(
          span(
            iconTag(Icon.Trophy)(cls := "text"),
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
            if t.mode.rated then trans.site.ratedTournament() else trans.site.casualTournament(),
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
        tr(
          td(cls := "name")(
            a(cls := "text", dataIcon := tournamentIcon(tour), href := routes.Tournament.show(tour.id)):
              tour.name(full = false)
          ),
          td(
            if tour.isStarted then timeRemaining(tour.finishesAt)
            else momentFromNow(tour.schedule.fold(tour.startsAt)(_.at.instant))
          ),
          td(tour.durationString),
          tour.conditions.teamMember match
            case Some(t) =>
              td(dataIcon := Icon.Group, cls := "text tour-team-icon", title := t.teamName)(visiblePlayers)
            case _ if tour.isTeamBattle =>
              td(dataIcon := Icon.Group, cls := "text tour-team-icon", title := trans.team.teamBattle.txt()):
                visiblePlayers
            case None => td(dataIcon := Icon.User, cls := "text")(visiblePlayers)
        )
    )

  def tournamentLink(tour: Tournament)(using Translate): Frag =
    a(
      dataIcon := Icon.Trophy.value,
      cls      := (if tour.isScheduled then "text is-gold" else "text"),
      href     := routes.Tournament.show(tour.id).url
    )(tour.name())

  def tournamentLink(tourId: TourId)(using Translate): Frag =
    a(
      dataIcon := Icon.Trophy.value,
      cls      := "text",
      href     := routes.Tournament.show(tourId).url
    )(tournamentIdToName(tourId))

  def tournamentIdToName(id: TourId)(using translate: Translate): String =
    getTourName.sync(id)(using translate.lang).getOrElse("Tournament")

  object scheduledTournamentNameShortHtml:
    private def icon(c: Icon) = s"""<span data-icon="$c"></span>"""
    private val replacements =
      given lila.core.i18n.Translate = transDefault
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

  def scheduleJsI18n(using Context) = i18nJsObject(schedulei18nKeys)

  def jsI18n(tour: Tournament)(using Context) = i18nJsObject(
    i18nKeys ++ (tour.isTeamBattle.so(teamBattleI18nKeys))
  )

  private val i18nKeys = List(
    trans.site.standing,
    trans.site.starting,
    trans.swiss.startingIn,
    trans.site.tournamentIsStarting,
    trans.site.youArePlaying,
    trans.site.standByX,
    trans.site.tournamentPairingsAreNowClosed,
    trans.site.join,
    trans.site.pause,
    trans.site.withdraw,
    trans.site.joinTheGame,
    trans.site.signIn,
    trans.site.averageElo,
    trans.site.gamesPlayed,
    trans.site.nbPlayers,
    trans.site.winRate,
    trans.site.berserkRate,
    trans.study.downloadAllGames,
    trans.site.performance,
    trans.site.tournamentComplete,
    trans.site.movesPlayed,
    trans.site.whiteWins,
    trans.site.blackWins,
    trans.site.drawRate,
    trans.site.nextXTournament,
    trans.site.averageOpponent,
    trans.site.tournamentEntryCode,
    trans.site.topGames
  )

  private val teamBattleI18nKeys = List(
    trans.arena.viewAllXTeams,
    trans.site.players,
    trans.arena.averagePerformance,
    trans.arena.averageScore,
    trans.team.teamPage,
    trans.arena.pickYourTeam,
    trans.arena.whichTeamWillYouRepresentInThisBattle,
    trans.arena.youMustJoinOneOfTheseTeamsToParticipate
  )

  private val schedulei18nKeys = List(
    trans.site.ratedTournament,
    trans.site.casualTournament
  )
