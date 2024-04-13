package views.html.tournament

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }
import lila.core.i18n.I18nKey as trans
import lila.tournament.Tournament

object bits:

  def notFound()(using PageContext) =
    views.html.base.layout(
      title = trans.site.tournamentNotFound.txt()
    ) {
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.tournamentNotFound()),
        p(trans.site.tournamentDoesNotExist()),
        p(trans.site.tournamentMayHaveBeenCanceled()),
        br,
        br,
        a(href := routes.Tournament.home)(trans.site.returnToTournamentsHomepage())
      )
    }

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

  def userPrizeDisclaimer(ownerId: UserId) =
    (!env.web.settings.prizeTournamentMakers
      .get()
      .value
      .contains(ownerId))
      .option(
        div(cls := "tour__prize")(
          "This tournament is not organized by Lichess.",
          br,
          "If it has prizes, Lichess is not responsible for paying them."
        )
      )

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
