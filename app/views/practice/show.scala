package views.html
package practice

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    us: lila.practice.UserStudy,
    data: lila.practice.JsonView.JsData
  )(implicit ctx: Context) = views.html.base.layout(
    title = us.practiceStudy.name,
    side = div(cls := "side_box study_box").toHtml.some,
    moreCss = cssTags("analyse.css", "study.css", "practice.css"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lichess = lichess || {}; lichess.practice = {
practice: ${safeJsonValue(data.practice)},
study: ${safeJsonValue(data.study)},
data: ${safeJsonValue(data.analysis)},
i18n: ${board.userAnalysisI18n()},
explorer: {
endpoint: "$explorerEndpoint",
tablebaseEndpoint: "$tablebaseEndpoint"
}};""")
    ),
    chessground = false,
    zoomable = true
  ) {
      div(cls := "analyse cg-512")(miniBoardContent)
    }
}
