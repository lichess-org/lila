package views.html
package practice

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    us: lidraughts.practice.UserStudy,
    data: lidraughts.practice.JsonView.JsData
  )(implicit ctx: Context) = views.html.base.layout(
    title = us.practiceStudy.name,
    side = div(cls := "side_box study_box").toHtml.some,
    moreCss = cssTags("analyse.css", "study.css", "practice.css"),
    moreJs = frag(
      jsAt(s"compiled/lidraughts.analyse${isProd ?? (".min")}.js"),
      embedJs(s"""lidraughts = lidraughts || {}; lidraughts.practice = {
practice: ${safeJsonValue(data.practice)},
study: ${safeJsonValue(data.study)},
data: ${safeJsonValue(data.analysis)},
i18n: ${board.userAnalysisI18n()},
explorer: {
endpoint: "$explorerEndpoint",
tablebaseEndpoint: "$tablebaseEndpoint"
}};""")
    ),
    draughtsground = false,
    zoomable = true
  ) {
      div(cls := "analyse cg-512")(miniBoardContent)
    }
}
