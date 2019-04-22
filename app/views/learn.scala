package views.html.learn

import play.api.libs.json.JsObject

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object index {

  def apply(data: Option[play.api.libs.json.JsValue])(implicit ctx: Context) = views.html.base.layout(
    title = s"${trans.learn.learnDraughts.txt()} - ${trans.learn.byPlaying.txt()}",
    moreJs = frag(
      jsAt(s"compiled/lidraughts.learn${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""$$(function() {
LidraughtsLearn(document.getElementById('learn-app'), {
data: ${data.fold("null")(safeJsonValue)},
i18n: ${safeJsonValue(i18nFullDbJsObject(lidraughts.i18n.I18nDb.Learn))}});});""")
    ),
    moreCss = cssTag("learn"),
    draughtsground = false,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Learn draughts by playing",
      description = "You don't know anything about draughts? Excellent! Let's have fun and learn to play draughts!",
      url = s"$netBaseUrl${routes.Lobby.home}"
    ).some,
    zoomable = true
  ) {
      main(id := "learn-app")
    }
}
