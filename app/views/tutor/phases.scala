package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorFullReport, TutorMetric, TutorMetricOption, TutorPerfReport, TutorRatio }

object phases {

  def apply(full: TutorFullReport.Available, report: TutorPerfReport, user: lila.user.User)(implicit
      ctx: Context
  ) =
    bits.layout(
      full,
      menu = frag(
        a(href := routes.Tutor.user(user.username))("Tutor"),
        a(href := routes.Tutor.openings(user.username, report.perf.key))("Openings"),
        a(href := routes.Tutor.phases(user.username, report.perf.key), cls := "active")("Game phases")
      )
    )(
      cls := "box box-pad",
      h1(
        a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
        report.perf.trans,
        " phases"
      ),
      div(cls := "tutor__phases")(
        // bits.mascotSays(
        //   )
      )
    )
}
