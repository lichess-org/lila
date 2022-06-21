package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tutor.{ TutorFullReport, TutorMetric, TutorMetricOption, TutorPerfReport, TutorRatio }
import lila.insight.Phase

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
      cls := "tutor__phases box",
      h1(
        a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
        report.perf.trans,
        " phases"
      ),
      bits.mascotSays(
        ul(report phaseHighlights 3 map compare.show)
      ),
      div(cls := "tutor-cards tutor-cards--full-size")(
        report.phases.map { phase =>
          div(cls := "tutor-card tutor__phases__phase")(
            div(cls := "tutor-card__top")(
              div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
                h3(cls := "tutor-card__top__title__text")(phase.phase.name)
              )
            ),
            div(cls := "tutor-card__content")(
              phase.acpl.toString
            )
          )
        }
      )
    )
}
