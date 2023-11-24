package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKeys => trans }
import lila.tournament.Tournament

import controllers.routes

object bits {

  def notFound()(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.tournamentNotFound.txt()
    ) {
      main(cls := "page-small box box-pad")(
        h1(trans.tournamentNotFound()),
        p(trans.tournamentDoesNotExist()),
        p(trans.tournamentMayHaveBeenCanceled()),
        br,
        br,
        a(href := routes.Tournament.home)(trans.returnToTournamentsHomepage())
      )
    }

  def enterable(tours: List[Tournament]) =
    table(cls := "tournaments")(
      tours map { tour =>
        tr(
          td(cls := "name")(
            a(cls := "text", dataIcon := tournamentIconChar(tour), href := routes.Tournament.show(tour.id))(
              tour.name
            )
          ),
          tour.schedule.fold(td) { s =>
            td(momentFromNow(s.at))
          },
          td(tour.durationString),
          td(dataIcon := "r", cls := "text")(tour.nbPlayers)
        )
      }
    )

  def userPrizeDisclaimer =
    div(cls := "tour__prize")(
      "This tournament is NOT organized by Lishogi.",
      br,
      "If it has prizes, Lishogi is NOT responsible for paying them."
    )

  def scheduleJsI18n(implicit ctx: Context) = i18nJsObject(schedulei18nKeys)

  def jsI18n(tour: Tournament)(implicit ctx: Context) = i18nJsObject(
    i18nKeys ++ (tour.isTeamBattle ?? teamBattleI18nKeys)
  )

  private val i18nKeys = List(
    trans.black,
    trans.white,
    trans.sente,
    trans.gote,
    trans.shitate,
    trans.uwate,
    trans.standing,
    trans.starting,
    trans.tournamentIsStarting,
    trans.youArePlaying,
    trans.standByX,
    trans.tournamentPairingsAreNowClosed,
    trans.join,
    trans.pause,
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
    trans.xWins,
    trans.draws,
    trans.nextXTournament,
    trans.averageOpponent,
    trans.password,
    trans.standard,
    trans.minishogi,
    trans.chushogi,
    trans.annanshogi,
    trans.kyotoshogi,
    trans.checkshogi
  ).map(_.key)

  private val teamBattleI18nKeys = List(
    trans.arena.viewAllXTeams,
    trans.arena.averagePerformance,
    trans.arena.averageScore,
    trans.team.teamPage
  ).map(_.key)

  private val schedulei18nKeys = List(
    trans.ratedTournament,
    trans.casualTournament
  ).map(_.key)

}
