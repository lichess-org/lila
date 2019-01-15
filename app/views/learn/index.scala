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
    side = Some(div(id := "learn_side", cls := "side_box")),
    moreJs = frag(
      jsAt(s"compiled/lidraughts.learn${isProd ?? (".min")}.js"),
      embedJs(s"""$$(function() {
LidraughtsLearn(document.getElementById('learn_app'), {
data: ${data.fold("null")(safeJsonValue)},
sideElement: document.getElementById('learn_side'),
i18n: ${safeJsonValue(i18nFullDbJsObject(lidraughts.i18n.I18nDb.Learn))}});});""")
    ),
    moreCss = cssTag("learn.css"),
    draughtsground = false,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Learn draughts by playing",
      description = "You don't know anything about draughts? Excellent! Let's have fun and learn to play draughts!",
      url = s"$netBaseUrl${routes.Lobby.home}"
    ).some,
    zoomable = true
  ) {
      div(id := "learn_app", cls := "learn cg-512")
    }
}
