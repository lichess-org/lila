package views.html.learn

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object index {

  def apply(data: Option[play.api.libs.json.JsValue])(implicit ctx: Context) = views.html.base.layout(
    title = s"${trans.learn.learnChess.txt()} - ${trans.learn.byPlaying.txt()}",
    moreJs = frag(
      jsAt(s"compiled/lichess.learn${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""$$(function() {
LichessLearn(document.getElementById('learn-app'), ${
        safeJsonValue(Json.obj(
          "data" -> data,
          "i18n" -> i18nFullDbJsObject(lila.i18n.I18nDb.Learn)
        ))
      })})""")
    ),
    moreCss = cssTag("learn"),
    openGraph = lila.app.ui.OpenGraph(
      title = "Learn chess by playing",
      description = "You don't know anything about chess? Excellent! Let's have fun and learn to play chess!",
      url = s"$netBaseUrl${routes.Learn.index}"
    ).some,
    zoomable = true
  ) {
      main(id := "learn-app")
    }
}
