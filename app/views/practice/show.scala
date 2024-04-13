package views.html
package practice

import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.*

object show:

  def apply(
      us: lila.practice.UserStudy,
      data: lila.practice.JsonView.JsData
  )(using PageContext) =
    views.html.base.layout(
      title = us.practiceStudy.name.value,
      moreCss = cssTag("analyse.practice"),
      modules = List(analyseNvuiTag),
      pageModule = PageModule(
        "analyse.study",
        Json.obj(
          "practice" -> data.practice,
          "study"    -> data.study,
          "data"     -> data.analysis,
          "i18n"     -> (board.userAnalysisI18n() ++ i18nJsObject(study.jsI18n.gamebookPlayKeys))
        ) ++ views.html.board.bits.explorerAndCevalConfig
      ).some,
      csp = analysisCsp.some,
      zoomable = true
    ):
      main(cls := "analyse")
