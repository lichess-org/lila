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
        a(href := routes.Tutor.user(user.username))("Tutor"),
        a(href := routes.Tutor.perf(user.username, perfReport.perf.key))(perfReport.perf.trans),
        a(href := routes.Tutor.openings(user.username, perfReport.perf.key))("Openings"),
        perfReport.openings(as).families map { family =>
          a(
            href := routes.Tutor
              .opening(user.username, perfReport.perf.key, as.name, report.family.key.value),
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
          strong(perfReport.openingFrequency(as, report).percent.toInt, "%"),
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
        cls := "box__pad"
      )(
        bits.peerGradeWithDetail(concept.performance, report.performance.toOption, InsightPosition.Game),
        bits.peerGradeWithDetail(concept.accuracy, report.accuracy, InsightPosition.Move),
        bits.peerGradeWithDetail(concept.tacticalAwareness, report.awareness, InsightPosition.Move)
      )
    )
  }
}
