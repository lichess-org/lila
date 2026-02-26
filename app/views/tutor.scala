package views.tutor

import lila.app.UiEnv.{ *, given }

val bits = lila.tutor.ui.TutorBits(helpers)(views.opening.bits.openingUrl)
val perf = lila.tutor.ui.TutorPerfUi(helpers, bits)
val queue = lila.tutor.ui.TutorQueueUi(helpers, bits)
val reports = lila.tutor.ui.TutorReportsUi(helpers, bits)
val report = lila.tutor.ui.TutorReportUi(helpers, bits, perf)
val home = lila.tutor.ui.TutorHomeUi(helpers, bits, queue, reports)
val openingUi = lila.tutor.ui.TutorOpening(helpers, bits, perf)

def opening(
    full: lila.tutor.TutorFullReport,
    perfReport: lila.tutor.TutorPerfReport,
    report: lila.tutor.TutorOpeningFamily,
    as: Color,
    puzzle: Option[lila.puzzle.PuzzleOpening.FamilyWithCount]
)(using Context) =
  val puzzleFrag = puzzle.map: p =>
    a(
      cls := "button button-no-upper text",
      dataIcon := Icon.ArcheryTarget,
      href := routes.Puzzle.angleAndColor(p.family.key.value, as.name)
    )("Train with puzzles")
  openingUi.opening(full, perfReport, report, as, puzzleFrag)
