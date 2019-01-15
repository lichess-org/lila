package views.html.tournament

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.i18n.{ I18nKeys => trans }

object jsI18n {

  def apply()(implicit ctx: Context) = safeJsonValue(i18nJsObject(translations))

  private val translations = List(
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
    trans.viewMoreTournaments,
    trans.averageOpponent,
    trans.ratedTournament,
    trans.casualTournament
  )
}
