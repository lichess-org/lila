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
    bits.layout(full, menu = perf.menu(full, user, report, "time"))(
      cls := "tutor__time box",
      h1(
        a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
        report.perf.trans,
        " time management"
      ),
      bits.mascotSays(
        ul(report timeHighlights 5 map compare.show)
      ),
      div(cls := "tutor-card__content")(
        bits.peerComparison(concept.speed, report.globalClock),
        bits.peerComparison(concept.clockFlagVictory, report.flagging.win),
        bits.peerComparison(concept.clockTimeUsage, report.clockUsage)
      )
    )
}
