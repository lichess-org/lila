package views.html
package practice

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object show {

  def apply(
      us: lila.practice.UserStudy,
      data: lila.practice.JsonView.JsData
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = us.practiceStudy.name,
      moreCss = cssTag("analyse.practice"),
      moreJs = frag(
        analyseStudyTag,
        analyseNvuiTag,
        embedJsUnsafe(s"""lichess.practice=${safeJsonValue(
            Json.obj(
              "practice" -> data.practice,
              "study"    -> data.study,
              "data"     -> data.analysis,
              "i18n"     -> (board.userAnalysisI18n() ++ i18nJsObject(study.jsI18n.gamebookPlayKeys)),
              "explorer" -> views.html.board.bits.explorerConfig
            )
          )}""")
      ),
      csp = defaultCsp.withWebAssembly.withAnyWs.some,
      chessground = false,
      zoomable = true
    ) {
      main(cls := "analyse")
    }
}
