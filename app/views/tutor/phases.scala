package views.html.tutor

import controllers.routes
import play.api.libs.json.*

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tutor.{ TutorFullReport, TutorPerfReport }
import lila.insight.Phase
import lila.insight.InsightPosition

object phases:

  def apply(full: TutorFullReport.Available, report: TutorPerfReport, user: lila.user.User)(using
      ctx: Context
  ) =
    bits.layout(full, menu = perf.menu(full, user, report, "phases"))(
      cls := "tutor__phases box",
      boxTop(
        h1(
          a(href := routes.Tutor.perf(user.username, report.perf.key), dataIcon := "", cls := "text"),
          report.perf.trans,
          " phases"
        )
      ),
      bits.mascotSays(
        ul(report phaseHighlights 3 map compare.show)
      ),
      div(cls := "tutor-cards tutor-cards--triple")(
        report.phases.map { phase =>
          st.section(cls := "tutor-card tutor__phases__phase")(
            div(cls := "tutor-card__top")(
              div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
                h2(cls := "tutor-card__top__title__text")(phase.phase.name)
              )
            ),
            div(cls := "tutor-card__content")(
              grade.peerGradeWithDetail(concept.accuracy, phase.accuracy, InsightPosition.Move),
              grade.peerGradeWithDetail(concept.tacticalAwareness, phase.awareness, InsightPosition.Move),
              a(
                cls      := "button button-no-upper text",
                dataIcon := "",
                href     := routes.Puzzle.show(phase.phase.name)
              )("Train with ", phase.phase.name, " puzzles")
            )
          )
        }
      )
    )
