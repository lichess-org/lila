package views.html.tutor

import controllers.routes
import play.api.libs.json._
import play.api.mvc.Call

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorMetric, TutorMetricOption, TutorPerfReport, TutorRatio, TutorReport }
import lila.user.User

object perf {

  def apply(full: TutorReport.Available, report: TutorPerfReport, user: User)(implicit
      ctx: Context
  ) =
    bits.layout(full, menu = menu(full, user, report.some))(
      cls := "tutor__perf box box-pad",
      h1(
        a(href := routes.Tutor.user(user.username), dataIcon := "", cls := "text"),
        report.perf.trans
      ),
      div(cls := "tutor__perf__angles")(
        angleCard(routes.Tutor.openings(user.username, report.perf.key))(
          h2("Openings")
        ),
        angleCard(routes.Tutor.phases(user.username, report.perf.key))(
          h2("Game phases")
        )
      )
    )

  private[tutor] def menu(full: TutorReport.Available, user: User, report: Option[TutorPerfReport])(implicit
      ctx: Context
  ) = frag(
    a(href := routes.Tutor.user(user.username), cls := report.isEmpty.option("active"))("Tutor"),
    full.report.perfs.map { p =>
      a(
        cls  := p.perf.key.active(report.??(_.perf.key)),
        href := routes.Tutor.perf(user.username, p.perf.key)
      )(p.perf.trans)
    }
  )

  private def angleCard(url: Call)(content: Modifier*)(implicit ctx: Context) =
    div(cls := "tutor__perf__angle tutor-card tutor-overlaid")(
      a(
        cls  := "tutor-overlay",
        href := url
      ),
      content
    )

}
