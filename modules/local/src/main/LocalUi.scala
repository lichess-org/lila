package lila.local
package ui

import play.api.libs.json.*
import play.api.i18n.Lang

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator
import lila.core.i18n.{ I18nKey as trans }
import lila.common.Json.given
import lila.common.String.html.safeJsonValue
import lila.local.GameSetup

final class LocalUi(helpers: Helpers):
  import helpers.{ *, given }

  def index(data: JsObject, devUi: Boolean)(using ctx: Context) =
    Page("")
      .copy(fullTitle = s"Play vs Bots".some)
      .js(
        PageModule(if devUi then "local.dev" else "local", data ++ Json.obj("i18n" -> i18nJsObject(i18nKeys)))
      )
      .js(EsmInit("round"))
      .css(if devUi then "local.dev" else "local")
      .css("round")
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .css(ctx.pref.hasVoice.option("voice"))
      .csp(_.withWebAssembly)
      .graph(
        OpenGraph(
          title = "Play vs Bots",
          description = "Play vs Bots",
          url = netBaseUrl.value
        )
      )
      .zoom
      .hrefLangs(lila.ui.LangPath("/")) {
        emptyFrag
      }

//def i18n(using Translate) = i18nJsObject(i18nKeys)

  private val i18nKeys = Vector(
    trans.site.oneDay,
    trans.site.nbDays,
    trans.site.nbHours,
    trans.site.nbSecondsToPlayTheFirstMove,
    trans.site.kingInTheCenter,
    trans.site.threeChecks,
    trans.site.variantEnding,
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
