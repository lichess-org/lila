package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorMetric, TutorMetricOption, TutorRatio, TutorReport }
import lila.user.User

object home {

  def apply(report: TutorReport.Availability, user: User)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("tutor")),
      title = "Lichess Tutor"
    ) {
      main(cls := "page-menu tutor")(
        st.aside(cls := "page-menu__menu")(),
        div(cls := "page-menu__content tutor__home box box-pad")(
          h1("Lichess Tutor"),
          report match {
            case TutorReport.Available(report) => available(report, user)
            case a                             => a.toString
          }
        )
      )
    }

  private def available(report: TutorReport, user: User)(implicit ctx: Context) =
    div(cls := "tutor__perfs")(
      report.perfs.toList.map { perfReport =>
        st.article(cls := "tutor__perfs__perf tutor-card tutor-overlaid")(
          a(
            cls  := "tutor-overlay",
            href := routes.Tutor.perf(user.username, perfReport.perf.key)
          ),
          h3("Your ", perfReport.perf.trans, " games"),
          table(cls := "slist")(
            tbody(
              tr(
                th("Average rating"),
                td(perfReport.stats.rating.value)
              ),
              perfReport.estimateTotalTime map { time =>
                tr(
                  th("Total time playing"),
                  td(showMinutes(time.toMinutes.toInt))
                )
              },
              tr(
                th("Games played"),
                td(perfReport.stats.nbGames)
              )
            )
          )
        )
      }
    )
}
