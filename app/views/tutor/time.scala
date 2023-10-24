package views.html.tutor

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.insight.InsightPosition
import lila.tutor.TutorPeriodReport

object time:

  def apply(reports: TutorPeriodReport.UserReports, report: TutorPeriodReport)(using PageContext) =
    import reports.user
    bits.layout(menu = perf.menu(user, report, "time"))(
      cls := "tutor__time box",
      boxTop(
        h1(
          a(
            href     := routes.Tutor.perf(user.username, report.perf.key, report.id),
            dataIcon := licon.LessThan,
            cls      := "text"
          ),
          bits.otherUser(user),
          report.perf.trans,
          " time management"
        )
      ),
      bits.mascotSays(
        ul(report timeHighlights 5 map compare.show)
      ),
      div(cls := "tutor__pad")(
        grade.peerGradeWithDetail(concept.speed, report.globalClock, InsightPosition.Move),
        hr,
        grade.peerGradeWithDetail(concept.clockFlagVictory, report.flagging.win, InsightPosition.Game),
        hr,
        grade.peerGradeWithDetail(concept.clockTimeUsage, report.clockUsage, InsightPosition.Game)
      )
    )
