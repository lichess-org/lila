package views.html
package practice

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object show {

  def apply(
      us: lila.practice.UserStudy,
      data: lila.practice.JsonView.JsData
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = us.practiceStudy.name,
      moreCss = cssTag("analyse.practice"),
      moreJs = frag(
        ctx.blind option analyseNvuiTag,
        moduleJsTag(
          "analyse",
          Json.obj(
            "mode"     -> "practice",
            "practice" -> data.practice,
            "study"    -> data.study,
            "data"     -> data.analysis
          )
        )
      ),
      csp = defaultCsp.withWebAssembly.some,
      shogiground = false,
      zoomable = true
    ) {
      main(cls := "analyse")
    }
}
