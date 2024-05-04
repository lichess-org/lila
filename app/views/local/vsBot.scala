package views.local

import play.api.libs.json.{ JsObject, Json }
import play.api.i18n.Lang

import lila.app.UiEnv.{ *, given }
import lila.core.i18n.{ I18nKey as trans }
import lila.common.Json.given
import lila.common.String.html.safeJsonValue
import lila.game.Pov

object vsBot:
  def index(using ctx: Context) =
    Page("")
      .copy(fullTitle = s"$siteName • Play vs Bots".some)
      .js(
        PageModule(
          "local.vsBot",
          Json.obj(
            "mode" -> "vsBot",
            "pref" -> lila.pref.JsonView.write(ctx.pref, false),
            "i18n" -> i18n
          )
        )
      )
      .js(EsmInit("round"))
      .cssTag("vs-bot")
      .cssTag("round")
      .cssTag(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .cssTag(ctx.pref.hasVoice.option("voice"))
      .csp(_.withWebAssembly)
      .graph(
        OpenGraph(
          title = "Play vs Bots",
          description = "Play vs Bots",
          url = netBaseUrl.value
        )
      )
      .hrefLangs(lila.ui.LangPath("/")) {
        main(cls := "round")(
          st.aside(cls := "round__side")(
            st.section(id := "bot-view")(
              div(id := "bot-content")
            )
          ),
          // bits.roundAppPreload(pov),
          div(cls := "round__app")(
            div(cls := "round__app__board main-board")(),
            div(cls := "col1-rmoves-preload")
          ),
          div(cls := "round__underboard")(
            // bits.crosstable(cross, pov.game),
            // (playing.nonEmpty || simul.exists(_ isHost ctx.me)) option
            div(
              cls := "round__now-playing"
            )
          ),
          div(cls := "round__underchat")()
        )
      }
  def i18n(using Translate) = i18nJsObject(i18nKeys)
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
