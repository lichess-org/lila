package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorFullReport, TutorPerfReport }

object time {

  def apply(full: TutorFullReport.Available, report: TutorPerfReport, user: lila.user.User)(implicit
      ctx: Context
  ) =
    bits.layout(full, menu = perf.menu(full, user, report))(
      cls := "tutor__time box",
      h1(
        a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
        report.perf.trans,
        " time management"
      )
      // bits.mascotSays(
      //   ul(report phaseHighlights 3 map compare.show)
      // ),
      // div(cls := "tutor-cards tutor-cards--triple")(
      //   report.phases.map { phase =>
      //     st.section(cls := "tutor-card tutor__phases__phase")(
      //       div(cls := "tutor-card__top")(
      //         div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
      //           h2(cls := "tutor-card__top__title__text")(phase.phase.name)
      //         )
      //       ),
      //       div(cls := "tutor-card__content")(
      //         bits.peerComparison("Accuracy", phase.accuracy),
      //         bits.peerComparison("Tactical Awareness", phase.accuracy)
      //       )
      //     )
      //   }
      // )
    )
}
