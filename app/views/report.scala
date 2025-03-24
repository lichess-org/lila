package views.report

import lila.app.UiEnv.{ *, given }
import lila.report.ui.PendingCounts
import lila.rating.UserPerfsExt.bestPerfs
import lila.report.Report.WithSuspect
import lila.report.Room

val ui = lila.report.ui.ReportUi(helpers)(views.mod.ui.reportMenu)

def list(
    reports: List[WithSuspect],
    filter: String,
    scores: Room.Scores,
    pending: PendingCounts
)(using Context, Me) =
  ui.list.layout(filter, scores, pending)(views.mod.ui.reportMenu):
    ui.list.reportTable(reports)(
      bestPerfs = _.perfs.bestPerfs(2).map(showPerfRating),
      userMarks = views.mod.user.userMarks(_, none)
    )
