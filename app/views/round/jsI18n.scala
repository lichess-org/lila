package views.html.round

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }

object jsI18n {

  def apply(g: lila.game.Game)(implicit ctx: Context) =
    i18nJsObject {
      baseTranslations ++ {
        if (g.isCorrespondence) correspondenceTranslations
        else realtimeTranslations
      } ++ {
        g.variant.chushogi ?? chushogiTranslations
      } ++ {
        g.isTournament ?? tournamentTranslations
      } ++ {
        ctx.blind ?? nvuiTranslations
      }
    }

  private val correspondenceTranslations = Vector(
    trans.oneDay,
    trans.nbDays,
    trans.nbHours
  ).map(_.key)

  private val realtimeTranslations = Vector(trans.nbSecondsToPlayTheFirstMove).map(_.key)

  private val chushogiTranslations = Vector(
    trans.royalsLost,
    trans.bareKing,
    trans.gameAdjourned,
    trans.offerAdjournment,
    trans.adjournmentOfferSent,
    trans.yourOpponentOffersAnAdjournment,
    trans.offerResumption,
    trans.acceptResumption,
    trans.resumptionOfferSent,
    trans.yourOpponentProposesResumption,
    trans.makeASealedMove,
    trans.waitingForASealedMove
  ).map(_.key)

  private val tournamentTranslations = Vector(
    trans.backToTournament,
    trans.viewTournament,
    trans.standing
  ).map(_.key)

  private val baseTranslations = Vector(
    trans.black,
    trans.white,
    trans.sente,
    trans.gote,
    trans.shitate,
    trans.uwate,
    trans.flipBoard,
    trans.levelX,
    trans.yourTurn,
    trans.abortGame,
    trans.proposeATakeback,
    trans.offerDraw,
    trans.resign,
    trans.opponentLeftCounter,
    trans.opponentLeftChoices,
    trans.forceResignation,
    trans.forceDraw,
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
    trans.xResigned,
    trans.stalemate,
    trans.check,
    trans.repetition,
    trans.perpetualCheck,
    trans.xLeftTheGame,
    trans.xDidntMove,
    trans.draw,
    trans.impasse,
    trans.timeOut,
    trans.variantEnding,
    trans.xIsVictorious,
    trans.withdraw,
    trans.rematch,
    trans.rematchOfferSent,
    trans.rematchOfferAccepted,
    trans.waitingForOpponent,
    trans.cancelRematchOffer,
    trans.newOpponent,
    trans.confirmMove,
    trans.viewRematch,
    trans.xPlays,
    trans.giveNbSeconds,
    trans.preferences.giveMoreTime,
    trans.gameOver,
    trans.analysis,
    trans.postGameStudy,
    trans.standardStudy,
    trans.postGameStudyExplanation,
    trans.studyWithOpponent,
    trans.studyOfPlayers,
    trans.studyWith,
    trans.optional,
    trans.postGameStudiesOfGame,
    trans.study.createStudy,
    trans.study.searchByUsername,
    trans.yourOpponentWantsToPlayANewGameWithYou,
    trans.youPlayAsX,
    trans.itsYourTurn,
    trans.enteringKing,
    trans.invadingPieces,
    trans.totalImpasseValue,
    trans.fromPosition,
    trans.pressXtoFocus,
    trans.pressXtoSubmit
  ).map(_.key)
}
