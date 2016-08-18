package lila.mod

import org.joda.time.DateTime

import lila.db.dsl._
import lila.notify.{ Notification, NotifyApi }

private final class NotifyReporters(
    notifyApi: NotifyApi,
    reportColl: Coll) {

  def apply(user: lila.user.User): Funit =
    reportColl.distinct("createdBy", $doc(
      "user" -> user.id,
      "createdAt" -> $gt(DateTime.now minusDays 3),
      "createdBy" -> $ne("lichess")
    ).some) map lila.db.BSON.asStrings flatMap {
      _.map { reporterId =>
        notifyApi.addNotification(Notification(
          notifies = Notification.Notifies(reporterId),
          content = lila.notify.ReportedBanned))
      }.sequenceFu.void
    }
}
