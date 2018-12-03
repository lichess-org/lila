package views
package html.site

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.Scalatags._

object freeJs {

  private lazy val agpl = a(href := "https://www.gnu.org/licenses/agpl-3.0.en.html", cls := "blue")("AGPL-3.0+")

  private def github(path: String) = a(href := s"https://github.com/ornicar/lila/tree/master/$path", cls := "blue")(path)

  private val uiModules = List("site", "chat", "cli", "challenge", "notify", "learn", "insight", "editor", "puzzle", "round", "analyse", "lobby", "tournament", "tournamentSchedule", "tournamentCalendar", "simul", "perfStat", "dasher")

  def apply(implicit ctx: Context) = message(
    title = "LibreJS Validation Table",
    back = false
  ) {
    frag(
      p(
        "Here's the ",
        a(cls := "blue", href := "https://www.gnu.org/licenses/javascript-labels.en.html")(
          "JavaScript License Web Labels"
        ),
        " table,",
        br,
        "where you can find the source code for the website' scripts."
      ),
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
