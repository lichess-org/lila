package lila.mod

import lila.notify.{ Notification, NotifyApi }
import lila.user.User

private final class ModNotifier(
    notifyApi: NotifyApi,
    reportApi: lila.report.ReportApi
) {

  def reporters(user: User): Funit =
    reportApi.recentReportersOf(user) flatMap {
      _.map { reporterId =>
        notifyApi.addNotification(Notification.make(
          notifies = Notification.Notifies(reporterId),
          content = lila.notify.ReportedBanned
        ))
      }.sequenceFu.void
    }

  def refund(user: User, pt: lila.rating.PerfType, points: Int): Funit =
    notifyApi.addNotification(Notification.make(
      notifies = Notification.Notifies(user.id),
      content = lila.notify.RatingRefund(pt.name, points)
    ))
}
