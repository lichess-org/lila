package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorFullReport, TutorPerfReport }
import lila.user.User

object home {

  def apply(full: TutorFullReport.Available, user: User)(implicit ctx: Context) =
    bits.layout(full, menu = perf.menu(full, user, none))(
      cls := "tutor__home box",
      h1("Lichess Tutor"),
      bits.mascotSays(
        p(
          strong(
            cls := "tutor__intro",
            "Hello, I have examined ",
            full.report.nbGames.localize,
            " recent rated games of yours."
          )
        ),
        p(
          "You particularly enjoy playing ",
          full.report.favouritePerfs.map(_.perf.trans).mkString(" and "),
          "!"
        ),
        p("Let's see how to improve.")
      ),
      div(cls := "tutor__perfs")(
        full.report.perfs.toList map { perfReportCard(full.report, _, user) }
      )
    )

  private def perfReportCard(report: TutorFullReport, perfReport: TutorPerfReport, user: User)(implicit
      ctx: Context
  ) =
    st.article(cls := "tutor__perfs__perf tutor-card tutor-overlaid")(
      a(
        cls  := "tutor-overlay",
        href := routes.Tutor.perf(user.username, perfReport.perf.key)
      ),
      div(cls := "tutor-card__top")(
        iconTag(perfReport.perf.iconChar),
        h3(cls := "tutor-card__top__title")(
          perfReport.stats.nbGames.localize,
          " ",
          perfReport.perf.trans,
          " games"
        )
      ),
      div(cls := "tutor-card__content")(
        report ratioTimeOf perfReport.perf map { ratio =>
          p(strong(ratio.percent.toInt, "%"), " of your chess playing time.")
        },
        perfReport.highlights(5) map { highlight =>
          p(highlight.quality.wording.toString, " ", highlight.toString)
        }
      )
    )
}
