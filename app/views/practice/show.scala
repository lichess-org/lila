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
    responsive = true,
    moreCss = responsiveCssTag("analyse.practice"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lidraughts=window.lidraughts||{};lidraughts.practice={
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
      main(cls := "analyse")
    }
}
