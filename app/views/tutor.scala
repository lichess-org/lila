package views.tutor

import lila.app.UiEnv.{ *, given }

val bits = lila.tutor.ui.TutorBits(helpers)(views.opening.bits.openingUrl)
val perf = lila.tutor.ui.PerfUi(helpers, bits)
val home = lila.tutor.ui.TutorHome(helpers, bits, perf)
val openingUi = lila.tutor.ui.TutorOpening(helpers, bits, perf)

def opening(
    perfReport: lila.tutor.TutorPerfReport,
    report: lila.tutor.TutorOpeningFamily,
    as: Color,
    user: User,
    puzzle: Option[lila.puzzle.PuzzleOpening.FamilyWithCount]
)(using Context) =
  val puzzleFrag = puzzle.map: p =>
    a(
      cls := "button button-no-upper text",
      dataIcon := Icon.ArcheryTarget,
      href := routes.Puzzle.angleAndColor(p.family.key.value, as.name)
    )("Train with puzzles")
  openingUi.opening(perfReport, report, as, user, puzzleFrag)
