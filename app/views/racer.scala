package views.html

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.racer.RacerRace
import lila.user.User

object racer {

  def home(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("racer-home"),
      title = "Puzzle Racer"
    ) {
      main(cls := "page page-small racer-home box box-pad")(
        h1("Puzzle Racer"),
        div(cls := "racer-home__buttons")(
          postForm(cls := "racer-home__lobby", action := routes.Racer.lobby)(
            submitButton(cls := "button button-fat")(i(cls := "car")(0), "Join a public race")
          ),
          postForm(cls := "racer-home__create", action := routes.Racer.create)(
            submitButton(cls := "button button-fat")(i(cls := "car")(0), "Race your friends")
          )
        ),
        div(cls := "racer-home__about")(
          a(href := routes.Page.loneBookmark("racer"))("About Puzzle Racer")
        )
      )
    }

  def show(race: RacerRace, data: JsObject, pref: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("racer")),
      moreJs = frag(
        jsModule("racer"),
        embedJsUnsafeLoadThen(
          s"""LichessRacer.start(${safeJsonValue(
            Json.obj(
              "data" -> data,
              "pref" -> pref,
              "i18n" -> i18nJsObject(i18nKeys)
            )
          )})"""
        )
      ),
      title = "Puzzle Racer",
      zoomable = true,
      chessground = false
    ) {
      main(
        div(cls := "racer racer-app racer--play")(
          div(cls := "racer__board main-board"),
          div(cls := "racer__side")
        )
      )
    }

  private val i18nKeys = {
    import lila.i18n.I18nKeys.{ storm => s }
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
      s.yourRankX,
      s.waitForRematch,
      s.nextRace,
      s.joinRematch,
      s.waitingToStart,
      s.createNewGame,
      trans.toInviteSomeoneToPlayGiveThisUrl
    ).map(_.key)
  }
}
