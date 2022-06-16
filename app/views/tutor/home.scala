package views.html

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.tutor._
import chess.Color

object tutor {

  def home(report: TutorFullReport)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("tutor")),
      title = "Lichess Tutor"
    ) {
      main(
        h1("Lichess Tutor"),
        div(cls := "tutor tutor__report")(
          report.perfs.toList
            .filter(_._1 == lila.rating.PerfType.Blitz)
            .sortBy(-_._2.games.value)
            .map { case (pt, report) =>
              st.section(cls := "tutor__report__perf")(
                h2(pt.trans(ctx.lang)),
                h3("Openings"),
                Color.all.map { color =>
                  st.section(cls := "tutor__report__openings__color")(
                    h4(color.name),
                    report.openings(color).map { report =>
                      st.section(cls := "tutor__report__openings__opening")(
                        h5(report.opening.name),
                        p("Games: ", report.games.value),
                        p("Moves: ", report.moves.value)
                      )
                    }
                  )
                }
              )
            }
        )
      )
    }
}
