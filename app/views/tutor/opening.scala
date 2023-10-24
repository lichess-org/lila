package views.html.tutor

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.insight.InsightPosition
import lila.tutor.{ TutorOpeningFamily, TutorPeriodReport }

object opening:

  def apply(
      reports: TutorPeriodReport.UserReports,
      period: TutorPeriodReport,
      report: TutorOpeningFamily,
      as: chess.Color,
      puzzle: Option[lila.puzzle.PuzzleOpening.FamilyWithCount]
  )(using PageContext) =
    import reports.user
    bits.layout(
      title = s"Lichess Tutor • ${period.perf.trans} • ${as.name} • ${report.family.name.value}",
      menu = frag(
        period.openings(as).families map { family =>
          a(
            href := routes.Tutor
              .opening(user.username, period.perf.key, period.id, as.name, family.family.key.value),
            cls := family.family.key.value.active(report.family.key.value)
          )(family.family.name.value)
        }
      )
    )(
      cls := "tutor__opening box",
      boxTop(
        h1(
          a(
            href     := routes.Tutor.openings(user.username, period.perf.key, period.id),
            dataIcon := licon.LessThan,
            cls      := "text"
          ),
          bits.otherUser(user),
          period.perf.trans,
          ": ",
          report.family.name,
          " as ",
          as.name
        )
      ),
      bits.mascotSays(
        div(
          cls              := "lpv lpv--todo",
          st.data("pgn")   := report.family.anyOpening.pgn,
          st.data("title") := report.family.name
        ),
        div(cls := "mascot-says__content__text")(
          p(
            "You played the ",
            report.family.name.value,
            " in ",
            report.performance.mine.count.localize,
            " games, which is ",
            bits.percentFrag(period.openingFrequency(as, report)),
            " of the time you played as ",
            as.name,
            "."
          ),
          div(cls := "mascot-says__buttons")(
            a(
              cls      := "button button-no-upper text",
              dataIcon := licon.InfoCircle,
              href     := views.html.opening.bits.openingUrl(report.family.anyOpening)
            )("Learn about this opening"),
            a(
              cls      := "button button-no-upper text",
              dataIcon := licon.Book,
              href := s"${routes.UserAnalysis
                  .pgn(report.family.anyOpening.pgn.value.replace(" ", "_"))}#explorer/${user.username}"
            )("Personal opening explorer"),
            puzzle.map { p =>
              a(
                cls      := "button button-no-upper text",
                dataIcon := licon.ArcheryTarget,
                href     := routes.Puzzle.angleAndColor(p.family.key.value, as.name)
              )("Train with puzzles")
            }
          )
        )
      ),
      div(cls := "tutor__pad")(
        grade.peerGradeWithDetail(concept.performance, report.performance.toOption, InsightPosition.Game),
        hr,
        grade.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
        hr,
        grade.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move)
      )
    )
