package views.html

import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object storm {

  def home(json: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("storm")),
      moreJs = frag(
        jsModule("storm"),
        embedJsUnsafeLoadThen(
          s"""LichessStorm.start(${safeJsonValue(
            Json.obj(
              "data" -> json,
              "i18n" -> jsI18n
            )
          )})"""
        )
      ),
      title = "Puzzle Storm"
    ) {
      main(cls := "box storm")
    }

  def jsI18n(implicit ctx: Context) = i18nJsObject(i18nKeys)

  private val i18nKeys = List(
    trans.storm.moveToStart
  ).map(_.key)
}
