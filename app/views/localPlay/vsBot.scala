package views.html.localPlay

import controllers.routes
import play.api.libs.json.{ JsObject, Json }
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.i18n.{ I18nKeys as trans }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.Json.given
import lila.common.String.html.safeJsonValue
import lila.game.Pov
import views.html.round.bits

object vsBot:
  def index(using ctx: PageContext) =
    views.html.base.layout(
      title = "Play vs Bots",
      moreJs = frag(
        jsModuleInit(
          "localPlay",
          Json.obj(
            "mode" -> "vsBot",
            "pref" -> lila.pref.JsonView.write(ctx.pref, false),
            "i18n" -> i18n
          )
        ),
        jsModule("round")
      ),
      moreCss = frag(
        cssTag("vs-bot"),
        cssTag("round"),
        ctx.pref.hasKeyboardMove option cssTag("keyboardMove"),
        ctx.pref.hasVoice option cssTag("voice")
        // ctx.blind option cssTag("round.nvui"),
      ),
      csp = analysisCsp.some,
      openGraph = lila.app.ui
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
  def i18n(using Lang) = i18nJsObject(translations)
  val translations = Vector(
    trans.oneDay,
    trans.nbDays,
    trans.nbHours,
    trans.nbSecondsToPlayTheFirstMove,
    trans.kingInTheCenter,
    trans.threeChecks,
    trans.variantEnding,
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
    trans.cheatDetected,
    trans.whiteResigned,
    trans.blackResigned,
    trans.whiteDidntMove,
    trans.blackDidntMove,
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
  )
