package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorMetric, TutorMetricOption, TutorPerfReport, TutorRatio, TutorReport }

object phases {

  def apply(fullReport: TutorReport, report: TutorPerfReport, user: lila.user.User)(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("tutor")),
      title = "Lichess Tutor"
    ) {
      main(cls := "page-menu tutor")(
        st.aside(cls := "page-menu__menu subnav")(
          a(href := routes.Tutor.user(user.username))("Tutor"),
          a(href := routes.Tutor.openings(user.username, report.perf.key))("Openings"),
          a(href := routes.Tutor.phases(user.username, report.perf.key), cls := "active")("Game phases")
        ),
        div(cls := "page-menu__content box box-pad")(
          h1(
            a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
            report.perf.trans,
            " phases"
          ),
          div(cls := "tutor__phases")(
          )
        )
      )
    }
}
