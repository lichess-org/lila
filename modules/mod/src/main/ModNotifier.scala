package lila.mod

import lila.notify.{ Notification, NotifyApi }
import lila.report.{ Mod, Suspect, Victim }

private final class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi
) {

  def reporters(mod: Mod, sus: Suspect): Funit =
    reportApi.recentReportersOf(sus) flatMap {
      _.filter(r => mod.user.id != r.value).map { reporterId =>
        notifyApi.addNotification(Notification.make(
          notifies = Notification.Notifies(reporterId.value),
          content = lila.notify.ReportedBanned
        ))
      }.sequenceFu.void
    }

  def refund(victim: Victim, pt: lila.rating.PerfType, points: Int): Funit =
    notifyApi.addNotification(Notification.make(
      notifies = Notification.Notifies(victim.user.id),
      content = lila.notify.RatingRefund(pt.name, points)
    ))
}
