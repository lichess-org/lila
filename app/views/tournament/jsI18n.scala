package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.common.String.html.safeJsonValue
import lila.common.String.html.safeJson
import lila.i18n.{ I18nKeys => trans }

object jsI18n {

  def apply()(implicit ctx: Context) = safeJson(i18nJsObject(translations))

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
