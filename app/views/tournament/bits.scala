package views.html.tournament

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.i18n.{ I18nKeys as trans }
import lila.tournament.Tournament

import controllers.routes

object bits:

  def notFound()(using PageContext) =
    views.html.base.layout(
      title = trans.tournamentNotFound.txt()
    ) {
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.tournamentNotFound()),
        p(trans.tournamentDoesNotExist()),
        p(trans.tournamentMayHaveBeenCanceled()),
        br,
        br,
        a(href := routes.Tournament.home)(trans.returnToTournamentsHomepage())
      )
    }

  def enterable(tours: List[Tournament])(using Context) =
    table(cls := "tournaments")(
      tours.map: tour =>
        val visiblePlayers = tour.nbPlayers >= 10 option tour.nbPlayers
        tr(
          td(cls := "name")(
            a(cls := "text", dataIcon := tournamentIcon(tour), href := routes.Tournament.show(tour.id))(
              tour.name(full = false)
            )
          ),
          td(momentFromNow(tour.schedule.fold(tour.startsAt)(_.at.instant))),
          td(tour.durationString),
          tour.conditions.teamMember match
            case Some(t) =>
              td(dataIcon := licon.Group, cls := "text tour-team-icon", title := t.teamName)(visiblePlayers)
            case _ if tour.isTeamBattle =>
              td(dataIcon := licon.Group, cls := "text tour-team-icon", title := trans.team.teamBattle.txt())(
                visiblePlayers
              )
            case None => td(dataIcon := licon.User, cls := "text")(visiblePlayers)
        )
    )

  def userPrizeDisclaimer(ownerId: UserId) =
    !env.prizeTournamentMakers.get().value.contains(ownerId) option
      div(cls := "tour__prize")(
        "This tournament is not organized by Lichess.",
        br,
        "If it has prizes, Lichess is not responsible for paying them."
      )

  def scheduleJsI18n(using Context) = i18nJsObject(schedulei18nKeys)

  def jsI18n(tour: Tournament)(using Context) = i18nJsObject(
    i18nKeys ++ (tour.isTeamBattle so teamBattleI18nKeys)
  )

  private val i18nKeys = List(
    trans.standing,
    trans.starting,
    trans.tournamentIsStarting,
    trans.youArePlaying,
    trans.standByX,
    trans.tournamentPairingsAreNowClosed,
    trans.join,
    trans.withdraw,
    trans.joinTheGame,
    trans.signIn,
    trans.averageElo,
    trans.gamesPlayed,
    trans.nbPlayers,
    trans.winRate,
    trans.berserkRate,
    trans.performance,
    trans.tournamentComplete,
    trans.movesPlayed,
    trans.whiteWins,
    trans.blackWins,
    trans.draws,
    trans.nextXTournament,
    trans.averageOpponent,
    trans.tournamentEntryCode
  )

  private val teamBattleI18nKeys = List(
    trans.arena.viewAllXTeams,
    trans.arena.averagePerformance,
    trans.arena.averageScore,
    trans.team.teamPage
  )

  private val schedulei18nKeys = List(
    trans.ratedTournament,
    trans.casualTournament
  )
