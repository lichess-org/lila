package lila.notify

import lila.db.dsl._

private final class NotificationRepo(val coll: Coll) {

  import BSONHandlers._

  def insert(notification: Notification) : Funit = {
    coll.insert(notification).void
  }

  def markAllRead(notifies: Notification.Notifies) : Funit = {
    coll.update(unreadOnlyQuery(notifies), $set("read" -> true), multi=true).void
  }

  def unreadNotificationsCount(userId: Notification.Notifies) : Fu[Int] = {
    coll.count(unreadOnlyQuery(userId).some)
  }

  val recentSort = $sort desc "created"

  def userNotificationsQuery(userId: Notification.Notifies) = $doc("notifies" -> userId.value)

  private def unreadOnlyQuery(userId:Notification.Notifies) = $doc("notifies" -> userId.value, "read" -> false)

}