package views.tutor

import lila.app.templating.Environment.{ *, given }

import lila.tutor.{ TutorOpeningFamily, TutorPerfReport }

object opening:

  lazy val ui = lila.tutor.ui.TutorOpening(helpers, bits)

  def opening(
      perfReport: TutorPerfReport,
      report: TutorOpeningFamily,
      as: chess.Color,
      user: User,
      puzzle: Option[lila.puzzle.PuzzleOpening.FamilyWithCount]
  )(using PageContext) =
    layout(
      title = s"Lichess Tutor • ${perfReport.perf.trans} • ${as.name} • ${report.family.name.value}",
      menu = ui.openingMenu(perfReport, report, as, user)
    )(
      cls := "tutor__opening box",
      ui.opening(
        perfReport,
        report,
        as,
        user,
        puzzle.map: p =>
          a(
            cls      := "button button-no-upper text",
            dataIcon := Icon.ArcheryTarget,
            href     := routes.Puzzle.angleAndColor(p.family.key.value, as.name)
          )("Train with puzzles")
      )
    )

  def openings(report: TutorPerfReport, user: User)(using ctx: PageContext) =
    layout(menu = perf.ui.menu(user, report, "openings"))(
      cls := "tutor__openings box",
      ui.openings(report, user)
    )
