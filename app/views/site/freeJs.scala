package views
package html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object freeJs {

  private lazy val agpl = a(href := "https://www.gnu.org/licenses/agpl-3.0.en.html", cls := "blue")("AGPL-3.0+")

  private def github(path: String) = a(href := s"https://github.com/ornicar/lila/tree/master/$path", cls := "blue")(path)

  private val uiModules = List("site", "chat", "cli", "challenge", "notify", "learn", "insight", "editor", "puzzle", "round", "analyse", "lobby", "tournament", "tournamentSchedule", "tournamentCalendar", "simul", "perfStat", "dasher")

  private val title = "LibreJS Validation Table"

  def apply(implicit ctx: Context) = help.layout(
    title = title,
    active = "freeJs",
    moreCss = cssTag("slist")
  ) {
      main(cls := "small-page box box-pad")(
        h1(
          a(cls := "blue", href := "https://www.gnu.org/licenses/javascript-labels.en.html")(
            "JavaScript License Web Labels"
          ),
          " table"
        ),
        p("where you can find the source code for the website' scripts."),
        br, br,
        table(id := "jslicense-labels1", cls := "slist")(
          thead(
            tr(List("Script File", "License", "Source Code").map(th(_)))
          ),
          tbody(
            uiModules map { module =>
              val file = s"lichess.$module.min.js"
              tr(
                td(a(href := assetUrl(s"compiled/$file"), cls := "blue")(file)),
                td(agpl),
                td(github(s"ui/$module/src"))
              )
            }
          )
        )
      )
    }
}
