package views.html

import controllers.routes
import play.api.libs.json.*

import lila.api.WebContext
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.LangPath
import lila.common.String.html.safeJsonValue
import lila.i18n.I18nKeys.{ storm as s }

object racer:

  def home(using WebContext) =
    views.html.base.layout(
      moreCss = cssTag("racer-home"),
      title = "Puzzle Racer",
      withHrefLangs = LangPath(routes.Racer.home).some
    ) {
      main(cls := "page page-small racer-home box box-pad")(
        h1(cls := "box__top")("Puzzle Racer"),
        div(cls := "racer-home__buttons")(
          postForm(cls := "racer-home__lobby", action := routes.Racer.lobby)(
            submitButton(cls := "button button-fat")(i(cls := "car")(0), s.joinPublicRace())
          ),
          postForm(cls := "racer-home__create", action := routes.Racer.create)(
            submitButton(cls := "button button-fat")(i(cls := "car")(0), s.raceYourFriends())
          )
        ),
        div(cls := "racer-home__about")(
          a(href := routes.Page.loneBookmark("racer"))(trans.aboutX("Puzzle Racer"))
        )
      )
    }

  def show(data: JsObject)(using WebContext) =
    views.html.base.layout(
      moreCss = frag(cssTag("racer")),
      moreJs = frag(
        jsModule("racer"),
        embedJsUnsafeLoadThen(
          s"""LichessRacer.start(${safeJsonValue(
              data ++ Json.obj("i18n" -> i18nJsObject(i18nKeys))
            )})"""
        )
      ),
      title = "Puzzle Racer",
      zoomable = true,
      zenable = true,
      chessground = false
    ) {
      main(
        div(cls := "racer racer-app racer--play")(
          div(cls := "racer__board main-board"),
          div(cls := "racer__side")
        )
      )
    }

  private val i18nKeys =
    List(
      s.score,
      s.combo,
      s.youPlayTheWhitePiecesInAllPuzzles,
      s.youPlayTheBlackPiecesInAllPuzzles,
      s.getReady,
      s.waitingForMorePlayers,
      s.raceComplete,
      s.spectating,
      s.joinTheRace,
      s.startTheRace,
      s.yourRankX,
      s.waitForRematch,
      s.nextRace,
      s.joinRematch,
      s.waitingToStart,
      s.createNewGame,
      trans.toInviteSomeoneToPlayGiveThisUrl,
      s.skip,
      s.skipHelp,
      s.skipExplanation,
      s.puzzlesPlayed,
      s.failedPuzzles,
      s.slowPuzzles,
      s.skippedPuzzle,
      trans.flipBoard
    )
