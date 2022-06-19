package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorMetric, TutorMetricOption, TutorRatio, TutorReport }

object home {

  def apply(report: TutorReport, user: lila.user.User)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("tutor")),
      title = "Lichess Tutor"
    ) {
      main(cls := "page-menu tutor")(
        st.aside(cls := "page-menu__menu")(),
        div(cls := "page-menu__content tutor__home box box-pad")(
          h1("Lichess Tutor"),
          div(cls := "tutor__perfs")(
            report.perfs.map { perfReport =>
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
                    tr(
                      th("Games played"),
                      td(perfReport.stats.nbGames)
                    )
                  )
                )
              )
            }
          )
        )
      )
    }
}
