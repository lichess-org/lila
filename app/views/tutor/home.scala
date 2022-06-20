package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.TutorReport
import lila.user.User

object home {

  def apply(full: TutorReport.Available, user: User)(implicit ctx: Context) =
    bits.layout(full, menu = perf.menu(full, user, none))(
      cls := "tutor__home box box-pad",
      h1("Lichess Tutor"),
      div(cls := "tutor__perfs")(
        full.report.perfs.toList.map { perfReport =>
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
    )
}
