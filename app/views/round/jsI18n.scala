package views.html.round

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.i18n.{ I18nKeys => trans }

object jsI18n {

  def apply(g: lidraughts.game.Game)(implicit ctx: Context) = i18nJsObject {
    baseTranslations ++ {
      if (g.isCorrespondence) correspondenceTranslations
      else realtimeTranslations
    } ++ {
      g.variant.exotic ?? variantTranslations
    } ++ {
      g.isTournament ?? tournamentTranslations
    } ++ {
      g.isSimul ?? simulTranslations
    } ++ {
      g.metadata.drawLimit.isDefined ?? drawLimitTranslations
    }
  }

  private val correspondenceTranslations = Vector(
    trans.oneDay,
    trans.nbDays,
    trans.nbHours
  )

  private val realtimeTranslations = Vector(trans.nbSecondsToPlayTheFirstMove)
  private val drawLimitTranslations = Vector(trans.drawOffersAfterX, trans.drawOffersNotAllowed)

  private val variantTranslations = Vector(
    trans.promotion,
    trans.variantEnding
  )

  private val tournamentTranslations = Vector(
    trans.backToTournament,
    trans.viewTournament,
    trans.standing
  )

  private val simulTranslations = Vector(
    trans.nbVictories,
    trans.nbDraws,
    trans.nbGames,
    trans.succeeded,
    trans.failed,
    trans.simulTimeOut,
    trans.simulTimeOutDuration,
    trans.simulTimeOutExplanation,
    trans.nbMinutes
  )

  private val baseTranslations = Vector(
    trans.flipBoard,
    trans.aiNameLevelAiLevel,
    trans.yourTurn,
    trans.abortGame,
    trans.proposeATakeback,
    trans.offerDraw,
    trans.resign,
    trans.opponentLeftChoices,
    trans.forceResignation,
    trans.forceDraw,
    trans.threefoldRepetition,
    trans.claimADraw,
    trans.drawOfferSent,
    trans.cancel,
    trans.yourOpponentOffersADraw,
    trans.accept,
    trans.decline,
    trans.takebackPropositionSent,
    trans.yourOpponentProposesATakeback,
    trans.thisPlayerUsesDraughtsComputerAssistance,
    trans.gameAborted,
    trans.whiteResigned,
    trans.blackResigned,
    trans.whiteLeftTheGame,
    trans.blackLeftTheGame,
    trans.draw,
    trans.timeOut,
    trans.whiteIsVictorious,
    trans.blackIsVictorious,
    trans.withdraw,
    trans.rematch,
    trans.rematchOfferSent,
    trans.rematchOfferAccepted,
    trans.waitingForOpponent,
    trans.cancelRematchOffer,
    trans.newOpponent,
    trans.moveConfirmation,
    trans.viewRematch,
    trans.whitePlays,
    trans.blackPlays,
    trans.giveNbSeconds,
    trans.giveMoreTime,
    trans.gameOver,
    trans.analysis,
    trans.yourOpponentWantsToPlayANewGameWithYou,
    trans.backToSimul,
    trans.xComplete
  )
}
