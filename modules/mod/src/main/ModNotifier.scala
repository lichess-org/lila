package lila.mod

import lila.notify.NotifyApi
import lila.report.{ Mod, Suspect, Victim }

final private class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def reporters(mod: Mod, sus: Suspect): Funit =
    reportApi.recentReportersOf(sus) flatMap {
      _.filter(r => mod.user.id != r.value)
        .map { reporterId =>
          notifyApi.notifyOne(reporterId.value, lila.notify.ReportedBanned)
        }
        .sequenceFu
        .void
    }

  def refund(victim: Victim, pt: lila.rating.PerfType, points: Int): Funit = {
    implicit val lang = victim.user.realLang | lila.i18n.defaultLang
    notifyApi.notifyOne(victim.user.id, lila.notify.RatingRefund(perf = pt.trans, points))
  }.void
}
