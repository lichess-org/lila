package views.html.round

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }

object jsI18n {

  def apply(g: lila.game.Game)(implicit ctx: Context) = i18nJsObject {
    baseTranslations ++ {
      if (g.isCorrespondence) correspondenceTranslations
      else realtimeTranslations
    } ++ {
      g.variant.exotic ?? variantTranslations
    } ++ {
      g.isTournament ?? tournamentTranslations
    }
  }

  private val correspondenceTranslations = Vector(
    trans.oneDay,
    trans.nbDays,
    trans.nbHours
  )

  private val realtimeTranslations = Vector(trans.nbSecondsToPlayTheFirstMove)

  private val variantTranslations = Vector(
    trans.kingInTheCenter,
    trans.threeChecks,
    trans.variantEnding
  )

  private val tournamentTranslations = Vector(
    trans.backToTournament,
    trans.viewTournament,
    trans.standing
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
    trans.thisPlayerUsesChessComputerAssistance,
    trans.gameAborted,
    trans.checkmate,
    trans.whiteResigned,
    trans.blackResigned,
    trans.stalemate,
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
    trans.yourOpponentWantsToPlayANewGameWithYou
  )
}
