package views.html.localPlay

import controllers.routes
import play.api.libs.json.{ JsObject, Json }
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.core.i18n.{ I18nKey as trans }
import lila.web.ui.ScalatagsTemplate.*
import lila.common.Json.given
import lila.common.String.html.safeJsonValue
import lila.game.Pov
import views.html.round.bits

object vsBot:
  def index(using ctx: PageContext) =
    views.html.base.layout(
      title = "Play vs Bots",
      modules = jsModuleInit(
        "localPlay",
        Json.obj(
          "mode" -> "vsBot",
          "pref" -> lila.pref.JsonView.write(ctx.pref, false),
          "i18n" -> i18n
        )
      ) ++ jsModule("round"),
      moreCss = frag(
        cssTag("vs-bot"),
        cssTag("round"),
        ctx.pref.hasKeyboardMove.option(cssTag("keyboardMove")),
        ctx.pref.hasVoice.option(cssTag("voice"))
        // ctx.blind option cssTag("round.nvui"),
      ),
      csp = analysisCsp.some,
      openGraph = lila.web
        .OpenGraph(
          title = "Play vs Bots",
          description = "Play vs Bots",
          url = s"$netBaseUrl${controllers.routes.LocalPlay.vsBot}"
        )
        .some,
      zoomable = true
    ) {
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
