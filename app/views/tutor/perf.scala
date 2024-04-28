package views.tutor

import lila.app.templating.Environment.{ *, given }

import lila.tutor.{ TutorFullReport, TutorPerfReport }

object perf:

  lazy val ui = lila.tutor.ui.PerfUi(helpers, bits)

  def apply(full: TutorFullReport, report: TutorPerfReport, user: User)(using PageContext) =
    layout(menu = ui.menu(user, report, "perf"))(cls := "tutor__perf box", ui(full, report, user))

  def phases(report: TutorPerfReport, user: User)(using PageContext) =
    layout(menu = perf.ui.menu(user, report, "phases"))(
      cls := "tutor__phases box",
      ui.phases(report, user)
    )

  def skills(report: TutorPerfReport, user: User)(using PageContext) =
    layout(menu = perf.ui.menu(user, report, "skills"))(
      cls := "tutor__skills box",
      ui.skills(report, user)
    )

  def time(report: TutorPerfReport, user: User)(using PageContext) =
    layout(menu = perf.ui.menu(user, report, "time"))(
      cls := "tutor__time box",
      ui.time(report, user)
    )
