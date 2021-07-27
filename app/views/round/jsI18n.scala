package views.html.round

import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }

object jsI18n {

  def apply(g: lila.game.Game)(implicit lang: Lang) =
    i18nJsObject {
      baseTranslations ++ {
        if (g.isCorrespondence) correspondenceTranslations
        else realtimeTranslations
      } ++ {
        g.variant.exotic ?? variantTranslations
      } ++ {
        g.isTournament ?? tournamentTranslations
      } ++ {
        g.isSwiss ?? swissTranslations
      }
    }

  private val correspondenceTranslations = Vector(
    trans.oneDay,
    trans.nbDays,
    trans.nbHours
  ).map(_.key)

  private val realtimeTranslations = Vector(trans.nbSecondsToPlayTheFirstMove).map(_.key)

  private val variantTranslations = Vector(
    trans.kingInTheCenter,
    trans.threeChecks,
    trans.variantEnding
  ).map(_.key)

  private val tournamentTranslations = Vector(
    trans.backToTournament,
    trans.viewTournament,
    trans.standing
  ).map(_.key)

  private val swissTranslations = Vector(
    trans.backToTournament,
    trans.viewTournament
  ).map(_.key)

  private val baseTranslations = Vector(
    trans.anonymous,
    trans.flipBoard,
    trans.aiNameLevelAiLevel,
    trans.yourTurn,
    trans.abortGame,
    trans.proposeATakeback,
    trans.offerDraw,
    trans.resign,
    trans.opponentLeftCounter,
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
    trans.thisAccountViolatedTos,
    trans.gameAborted,
    trans.checkmate,
    trans.whiteResigned,
    trans.blackResigned,
    trans.stalemate,
    trans.whiteLeftTheGame,
    trans.blackLeftTheGame,
    trans.draw,
    trans.whiteTimeOut,
    trans.blackTimeOut,
    trans.whiteIsVictorious,
    trans.blackIsVictorious,
    trans.drawByMutualAgreement,
    trans.fiftyMovesWithoutProgress,
    trans.insufficientMaterial,
    trans.withdraw,
    trans.rematch,
    trans.rematchOfferSent,
    trans.rematchOfferAccepted,
    trans.waitingForOpponent,
    trans.cancelRematchOffer,
    trans.newOpponent,
    trans.confirmMove,
    trans.viewRematch,
    trans.whitePlays,
    trans.blackPlays,
    trans.giveNbSeconds,
    trans.preferences.giveMoreTime,
    trans.gameOver,
    trans.analysis,
    trans.yourOpponentWantsToPlayANewGameWithYou,
    trans.youPlayTheWhitePieces,
    trans.youPlayTheBlackPieces,
    trans.itsYourTurn
  ).map(_.key)
}
