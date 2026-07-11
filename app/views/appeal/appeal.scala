package views.appeal

import lila.app.UiEnv.{ *, given }
import lila.appeal.Appeal
import lila.report.ui.PendingCounts
import lila.report.Report.Inquiry
import lila.core.misc.AppealTopic

lazy val ui = lila.appeal.ui.AppealUi(helpers)

lazy val tree = lila.appeal.ui.AppealTreeUi(helpers, ui)(topic =>
  preset => _ ?=> discussion.userForm(topic, lila.appeal.Appeal.form.fill(preset), isNew = true)
)

private lazy val queueUi = lila.appeal.ui.AppealQueueUi(helpers)

def queue(
    appeals: List[Appeal],
    inquiries: Map[UserId, Inquiry],
    topic: Option[AppealTopic],
    markedByMe: Set[UserId],
    scores: lila.report.Room.Scores,
    pending: PendingCounts
)(using Context, Me) =
  views.report.ui.list.layout("appeal", scores, pending, moreJs = esmInitBit("appealTopicSelect"))(
    views.mod.ui.reportMenu
  )(queueUi(appeals, inquiries.view.mapValues(_.mod).toMap, topic, markedByMe))
