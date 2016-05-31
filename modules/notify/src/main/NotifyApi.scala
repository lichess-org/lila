package lila.notify

import scala.concurrent.Future
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.hub.actorApi.SendTo
import lila.memo.AsyncCache

final class NotifyApi(bus: lila.common.Bus, repo: NotificationRepo) {

  import BSONHandlers.NotificationBSONHandler
  import JSONHandlers._

  def getNotifications(userId: Notification.Notifies, page: Int, perPage: Int) : Fu[Paginator[Notification]] = Paginator(
    adapter = new Adapter(
      collection = repo.coll,
      selector = repo.userNotificationsQuery(userId),
      projection = $empty,
      sort = repo.recentSort),
    currentPage = page,
    maxPerPage = perPage
  )

  def markAllRead(userId: Notification.Notifies) = repo.markAllRead(userId)

  def getUnseenNotificationCount = AsyncCache(repo.unreadNotificationsCount, maxCapacity = 20000)

  def addNotification(notification: Notification): Funit = {

    // Add to database and then notify any connected clients of the new notification
    insertOrDiscardNotification(notification) map {
      _ ?? {
        notif =>
            getUnseenNotificationCount(notif.notifies).
              map(NewNotification(notif, _)).
              foreach(notifyConnectedClients)
      }
    }
  }

  def addNotifications(notifications: List[Notification]) : Funit = {
    notifications.map(addNotification).sequenceFu.void
  }

  /**
    * Inserts notification into the repository.
    *
    * If the user already has an unread notification on the topic, discard it.
    *
    * If the user does not already have an unread notification on the topic, returns it unmodified.
    */
  private def insertOrDiscardNotification(notification: Notification): Fu[Option[Notification]] = {

    notification.content match {
      case MentionedInThread(_, _, topicId, _, _) => {
        repo.hasRecentUnseenNotifcationsInThread(notification.notifies, topicId).flatMap(alreadyNotified =>
          if (alreadyNotified) fuccess(None) else repo.insert(notification).inject(notification.some)
        )
      }
      case InvitedToStudy(invitedBy, _, studyId) => {
        repo.hasRecentUnseenStudyInvitation(notification.notifies, studyId).flatMap(alreadyNotified =>
          if (alreadyNotified) fuccess(None) else repo.insert(notification).inject(notification.some))
      }
    }
  }

  private def notifyConnectedClients(newNotification: NewNotification) : Unit = {
    val notificationsEventKey = "new_notification"
    val notificationEvent = SendTo(newNotification.notification.notifies.value, notificationsEventKey, newNotification)
    bus.publish(notificationEvent, 'users)
  }
}