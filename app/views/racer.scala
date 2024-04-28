package views.racer

import play.api.libs.json.*

import lila.app.templating.Environment.{ *, given }

import lila.core.i18n.I18nKey.storm as s

def home(using PageContext) =
  views.base.layout(
    moreCss = cssTag("racer-home"),
    title = "Puzzle Racer",
    withHrefLangs = lila.ui.LangPath(routes.Racer.home).some
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
        a(href := routes.Cms.lonePage("racer"))(trans.site.aboutX("Puzzle Racer"))
      )
    )
  }

def show(data: JsObject)(using PageContext) =
  views.base.layout(
    moreCss = cssTag("racer"),
    pageModule = PageModule("racer", data ++ Json.obj("i18n" -> i18nJsObject(i18nKeys))).some,
    title = "Puzzle Racer",
    zoomable = true,
    zenable = true
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
    trans.site.toInviteSomeoneToPlayGiveThisUrl,
    s.skip,
    s.skipHelp,
    s.skipExplanation,
    s.puzzlesPlayed,
    s.failedPuzzles,
    s.slowPuzzles,
    s.skippedPuzzle,
    trans.site.flipBoard
  )
