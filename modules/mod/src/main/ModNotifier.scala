package lila.mod

import org.joda.time.DateTime

import lila.db.dsl._
import lila.notify.{ Notification, NotifyApi }
import lila.user.User

private final class ModNotifier(
    notifyApi: NotifyApi,
    reportColl: Coll) {

  def reporters(user: User): Funit =
    reportColl.distinct[String, List]("createdBy", $doc(
      "user" -> user.id,
      "createdAt" -> $gt(DateTime.now minusDays 3),
      "createdBy" -> $ne("lichess")
    ).some) flatMap {
      _.map { reporterId =>
        notifyApi.addNotification(Notification(
          notifies = Notification.Notifies(reporterId),
          content = lila.notify.ReportedBanned))
      }.sequenceFu.void
    }

  def refund(user: User, pt: lila.rating.PerfType, points: Int): Funit =
    notifyApi.addNotification(Notification(
      notifies = Notification.Notifies(user.id),
      content = lila.notify.RatingRefund(pt.name, points)))
}
