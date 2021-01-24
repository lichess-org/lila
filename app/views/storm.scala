package views.html

import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object storm {

  def home(data: JsObject, pref: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("storm")),
      moreJs = frag(
        jsModule("storm"),
        embedJsUnsafeLoadThen(
          s"""LichessStorm.start(${safeJsonValue(
            Json.obj(
              "data" -> data,
              "pref" -> pref,
              "i18n" -> jsI18n
            )
          )})"""
        )
      ),
      title = "Puzzle Storm",
      zoomable = true
    ) {
      main(cls := "box storm")
    }

  def jsI18n(implicit ctx: Context) = i18nJsObject(i18nKeys)

  private val i18nKeys = {
    import lila.i18n.I18nKeys.{ storm => t }
    List(
      t.moveToStart,
      t.puzzlesSolved,
      t.newDailyHighscore,
      t.newAllTimeHighscore,
      t.previousHighscoreWasX,
      t.playAgain
    ).map(_.key)
  }
}
