package lila.notify

import scala.concurrent.duration._

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.hub.actorApi.socket.SendTo
import lila.user.UserRepo

final class NotifyApi(
    bus: lila.common.Bus,
    jsonHandlers: JSONHandlers,
    repo: NotificationRepo,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  import BSONHandlers.NotificationBSONHandler
  import jsonHandlers._

  val perPage = lila.common.MaxPerPage(7)

  def getNotifications(userId: Notification.Notifies, page: Int): Fu[Paginator[Notification]] = Paginator(
    adapter = new Adapter(
      collection = repo.coll,
      selector = repo.userNotificationsQuery(userId),
      projection = $empty,
      sort = repo.recentSort
    ),
    currentPage = page,
    maxPerPage = perPage
  )

  def getNotificationsAndCount(userId: Notification.Notifies, page: Int): Fu[Notification.AndUnread] =
    getNotifications(userId, page) zip unreadCount(userId) map (Notification.AndUnread.apply _).tupled

  def markAllRead(userId: Notification.Notifies) =
    repo.markAllRead(userId) >>- unreadCountCache.invalidate(userId)

  private val unreadCountCache = asyncCache.clearable(
    name = "notify.unreadCountCache",
    f = repo.unreadNotificationsCount,
    expireAfter = _.ExpireAfterAccess(15 minutes)
  )

  def unreadCount(userId: Notification.Notifies): Fu[Notification.UnreadCount] =
    unreadCountCache get userId map Notification.UnreadCount.apply

  def addNotification(notification: Notification): Funit =
    // Add to database and then notify any connected clients of the new notification
    insertOrDiscardNotification(notification) map {
      _ foreach { notif =>
        notifyUser(notif.notifies)
      }
    }

  def addNotificationWithoutSkipOrEvent(notification: Notification): Funit =
    repo.insert(notification) >>- unreadCountCache.invalidate(notification.notifies)

  def addNotifications(notifications: List[Notification]): Funit =
    notifications.map(addNotification).sequenceFu.void

  def remove(notifies: Notification.Notifies, selector: Bdoc): Funit =
    repo.remove(notifies, selector) >>- unreadCountCache.invalidate(notifies)

  def exists = repo.exists _

  private def shouldSkip(notification: Notification) =
    UserRepo.isKid(notification.notifies.value) >>| {
      notification.content match {
        case MentionedInThread(_, _, topicId, _, _) => repo.hasRecentNotificationsInThread(notification.notifies, topicId)
        case InvitedToStudy(invitedBy, _, studyId) => repo.hasRecentStudyInvitation(notification.notifies, studyId)
        case PrivateMessage(_, thread, _) => repo.hasRecentPrivateMessageFrom(notification.notifies, thread)
        case _ => fuFalse
      }
    }

  /**
   * Inserts notification into the repository.
   *
   * If the user already has an unread notification on the topic, discard it.
   *
   * If the user does not already have an unread notification on the topic, returns it unmodified.
   */
  private def insertOrDiscardNotification(notification: Notification): Fu[Option[Notification]] =
    shouldSkip(notification) flatMap {
      case true => fuccess(none)
      case false => addNotificationWithoutSkipOrEvent(notification) inject notification.some
    }

  private def notifyUser(notifies: Notification.Notifies): Funit =
    getNotificationsAndCount(notifies, 1) map { msg =>
      import play.api.libs.json.Json
      bus.publish(SendTo(notifies.value, "notifications", Json toJson msg), 'socketUsers)
    }
}
