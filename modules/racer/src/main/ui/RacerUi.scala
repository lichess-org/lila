package lila.racer
package ui

import play.api.libs.json.*

import lila.core.id.CmsPageKey
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class RacerUi(helpers: Helpers):
  import helpers.{ *, given }
  import trans.{ storm as s }

  def home(using Context) =
    Page("Puzzle Racer")
      .css("racer.home")
      .hrefLangs(LangPath(routes.Racer.home)):
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
            a(href := routes.Cms.lonePage(CmsPageKey("racer")))(trans.site.aboutX("Puzzle Racer"))
          )
        )

  def show(data: JsObject)(using Context) =
    Page("Puzzle Racer")
      .css("racer")
      .js(PageModule("racer", data ++ Json.obj("i18n" -> i18nJsObject(i18nKeys))))
      .zoom
      .zen:
        main(
          div(cls := "racer racer-app racer--play")(
            div(cls := "racer__board main-board"),
            div(cls := "racer__side")
          )
        )

  private val i18nKeys: List[lila.core.i18n.I18nKey] = List(
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
