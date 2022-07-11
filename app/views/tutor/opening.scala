package views.html.tutor

import controllers.routes
import play.api.libs.json._

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.LilaOpeningFamily
import lila.insight.InsightPosition
import lila.tutor.{ TutorFullReport, TutorOpeningFamily, TutorPerfReport }

object opening {

  def apply(
      full: TutorFullReport.Available,
      perfReport: TutorPerfReport,
      report: TutorOpeningFamily,
      as: chess.Color,
      user: lila.user.User,
      puzzle: Option[lila.puzzle.PuzzleOpening.FamilyWithCount]
  )(implicit ctx: Context) = {
    bits.layout(
      full,
      title = s"Lichess Tutor • ${perfReport.perf.trans} • ${as.name} • ${report.family.name.value}",
      menu = frag(
        perfReport.openings(as).families map { family =>
          a(
            href := routes.Tutor
              .opening(user.username, perfReport.perf.key, as.name, family.family.key.value),
            cls := family.family.key.value.active(report.family.key.value)
          )(family.family.name.value)
        }
      )
    )(
      cls := "tutor__opening box",
      h1(
        a(
          href     := routes.Tutor.openings(user.username, perfReport.perf.key),
          dataIcon := "",
          cls      := "text"
        ),
        perfReport.perf.trans,
        ": ",
        report.family.name,
        " as ",
        as.name
      ),
      bits.mascotSays(
        p(
          "You played the ",
          report.family.name.value,
          " in ",
          report.performance.mine.count.localize,
          " games, which is ",
          bits.percentFrag(perfReport.openingFrequency(as, report)),
          " of the time you played as ",
          as.name,
          "."
        ),
        puzzle.map { p =>
          a(
            cls  := "button button-no-upper",
            href := routes.Puzzle.angleAndColor(p.family.key.value, as.name)
          )(
            "Train your tactical awareness with ",
            p.family.name.value,
            " puzzles"
          )
        }
      ),
      div(
        cls := "tutor__pad"
      )(
        grade.peerGradeWithDetail(concept.performance, report.performance.toOption, InsightPosition.Game),
        hr,
        grade.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
        hr,
        grade.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move)
      )
    )
  }
}
