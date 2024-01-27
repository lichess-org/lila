package views.html.tutor

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tutor.TutorPerfReport
import lila.insight.{ Phase, InsightPosition }

object phases:

  def apply(report: TutorPerfReport, user: lila.user.User)(using PageContext) =
    bits.layout(menu = perf.menu(user, report, "phases"))(
      cls := "tutor__phases box",
      boxTop(
        h1(
          a(
            href     := routes.Tutor.perf(user.username, report.perf.key),
            dataIcon := licon.LessThan,
            cls      := "text"
          ),
          bits.otherUser(user),
          report.perf.trans,
          " phases"
        )
      ),
      bits.mascotSays(
        ul(report phaseHighlights 3 map compare.show)
      ),
      div(cls := "tutor-cards tutor-cards--triple")(
        report.phases.map: phase =>
          st.section(cls := "tutor-card tutor__phases__phase")(
            div(cls := "tutor-card__top")(
              div(cls := "tutor-card__top__title tutor-card__top__title--pad")(
                h2(cls := "tutor-card__top__title__text")(phase.phase.name)
              )
            ),
            div(cls := "tutor-card__content")(
              grade.peerGradeWithDetail(concept.accuracy, phase.accuracy, InsightPosition.Move),
              grade.peerGradeWithDetail(concept.tacticalAwareness, phase.awareness, InsightPosition.Move),
              div(cls := "tutor__phases__phase__buttons")(
                a(
                  cls      := "button button-no-upper text",
                  dataIcon := licon.ArcheryTarget,
                  href     := routes.Puzzle.show(phase.phase.name)
                )("Train with ", phase.phase.name, " puzzles"),
                a(
                  cls      := "button button-no-upper text",
                  dataIcon := licon.AnalogTv,
                  href     := s"${routes.Video.index}?tags=${phase.phase.name}"
                )("Watch ", phase.phase.name, " videos")
              ),
              phase.phase == Phase.Opening option
                a(cls := "tutor-card__more", href := routes.Tutor.openings(user.username, report.perf.key))(
                  "More about your ",
                  report.perf.trans,
                  " openings"
                )
            )
          )
      )
    )
