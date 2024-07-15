package lila.round
package ui

import lila.core.i18n.{ I18nKey, Translate }
import lila.ui.*

final class RoundI18n(helpers: Helpers):
  import helpers.*

  def apply(g: Game)(using t: Translate) =
    i18nJsObject:
      baseTranslations ++ {
        if g.isCorrespondence then correspondenceTranslations
        else realtimeTranslations
      } ++ {
        g.variant.exotic.so(variantTranslations)
      } ++ {
        g.isTournament.so(tournamentTranslations)
      } ++ {
        g.isSwiss.so(swissTranslations)
      }

  private val correspondenceTranslations = Vector(
    trans.site.oneDay,
    trans.site.nbDays,
    trans.site.nbHours
  )

  private val realtimeTranslations = Vector(trans.site.nbSecondsToPlayTheFirstMove)

  private val variantTranslations = Vector(
    trans.site.kingInTheCenter,
    trans.site.threeChecks,
    trans.site.variantEnding
  )

  private val tournamentTranslations = Vector(
    trans.site.backToTournament,
    trans.site.viewTournament,
    trans.site.standing
  )

  private val swissTranslations = Vector(
    trans.site.backToTournament,
    trans.site.viewTournament,
    trans.site.noDrawBeforeSwissLimit
  )

  private val baseTranslations = Vector(
    trans.site.anonymous,
    trans.site.flipBoard,
    trans.site.aiNameLevelAiLevel,
    trans.site.yourTurn,
    trans.site.abortGame,
    trans.site.proposeATakeback,
    trans.site.offerDraw,
    trans.site.resign,
    trans.site.opponentLeftCounter,
    trans.site.opponentLeftChoices,
    trans.site.forceResignation,
    trans.site.forceDraw,
    trans.site.threefoldRepetition,
    trans.site.claimADraw,
    trans.site.drawOfferSent,
    trans.site.cancel,
    trans.site.yourOpponentOffersADraw,
    trans.site.accept,
    trans.site.decline,
    trans.site.takebackPropositionSent,
    trans.site.yourOpponentProposesATakeback,
    trans.site.thisAccountViolatedTos,
    trans.site.gameAborted,
    trans.site.checkmate,
    trans.site.cheatDetected,
    trans.site.whiteResigned,
    trans.site.blackResigned,
    trans.site.whiteDidntMove,
    trans.site.blackDidntMove,
    trans.site.stalemate,
    trans.site.whiteLeftTheGame,
    trans.site.blackLeftTheGame,
    trans.site.draw,
    trans.site.whiteTimeOut,
    trans.site.blackTimeOut,
    trans.site.whiteIsVictorious,
    trans.site.blackIsVictorious,
    trans.site.drawByMutualAgreement,
    trans.site.fiftyMovesWithoutProgress,
    trans.site.insufficientMaterial,
    trans.site.pause,
    trans.site.withdraw,
    trans.site.rematch,
    trans.site.rematchOfferSent,
    trans.site.rematchOfferAccepted,
    trans.site.waitingForOpponent,
    trans.site.cancelRematchOffer,
    trans.site.newOpponent,
    trans.site.confirmMove,
    trans.site.viewRematch,
    trans.site.whitePlays,
    trans.site.blackPlays,
    trans.site.giveNbSeconds,
    trans.preferences.giveMoreTime,
    trans.site.gameOver,
    trans.site.analysis,
    trans.site.yourOpponentWantsToPlayANewGameWithYou,
    trans.site.youPlayTheWhitePieces,
    trans.site.youPlayTheBlackPieces,
    trans.site.itsYourTurn
  )
