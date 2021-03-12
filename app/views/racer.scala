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
      moreCss = frag(cssTag("racer")),
      title = "Puzzle Racer"
    ) {
      main(cls := "racer racer--home")(
        h1("Puzzle Racer"),
        div(
          postForm(cls := "racer__create", action := routes.Racer.create)(
            submitButton(cls := "button button-fat")("Race your friends")
          )
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
      s.moveToStart,
      s.puzzlesSolved,
      s.playAgain,
      s.score,
      s.moves,
      s.combo,
      s.newRun,
      trans.toInviteSomeoneToPlayGiveThisUrl
    ).map(_.key)
  }
}
