package views.html.learn

import play.api.libs.json.JsObject

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object index {

  def apply(data: Option[play.api.libs.json.JsValue])(implicit ctx: Context) = views.html.base.layout(
    title = s"${trans.learn.learnChess.txt()} - ${trans.learn.byPlaying.txt()}",
    side = Some(div(id := "learn_side", cls := "side_box")),
    moreJs = frag(
      jsAt(s"compiled/lichess.learn${isProd ?? (".min")}.js"),
      embedJs(s"""$$(function() {
LichessLearn(document.getElementById('learn_app'), {
data: ${data.fold("null")(safeJsonValue)},
sideElement: document.getElementById('learn_side'),
i18n: ${safeJsonValue(i18nFullDbJsObject(lila.i18n.I18nDb.Learn))}});});""")
    ),
    moreCss = cssTag("learn.css"),
    chessground = false,
    openGraph = lila.app.ui.OpenGraph(
      title = "Learn chess by playing",
      description = "You don't know anything about chess? Excellent! Let's have fun and learn to play chess!",
      url = s"$netBaseUrl${routes.Learn.index}"
    ).some,
    zoomable = true
  ) {
      div(id := "learn_app", cls := "learn cg-512")
    }
}
