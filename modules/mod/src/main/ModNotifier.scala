package lidraughts.mod

import lidraughts.notify.{ Notification, NotifyApi }
import lidraughts.report.{ Mod, Suspect, Victim }

private final class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lidraughts.report.ReportApi
) {

  def reporters(mod: Mod, sus: Suspect): Funit =
    reportApi.recentReportersOf(sus) flatMap {
      _.filter(r => mod.user.id != r.value).map { reporterId =>
        notifyApi.addNotification(Notification.make(
          notifies = Notification.Notifies(reporterId.value),
          content = lidraughts.notify.ReportedBanned
        ))
      }.sequenceFu.void
    }

  def refund(victim: Victim, pt: lidraughts.rating.PerfType, points: Int): Funit =
    notifyApi.addNotification(Notification.make(
      notifies = Notification.Notifies(victim.user.id),
      content = lidraughts.notify.RatingRefund(pt.name, points)
    ))
}
