package lila.notify

import scala.concurrent.duration._

import lila.common.Bus
import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.hub.actorApi.socket.SendTo
import lila.memo.CacheApi._
import lila.user.UserRepo
import lila.i18n.I18nLangPicker

final class NotifyApi(
    jsonHandlers: JSONHandlers,
    repo: NotificationRepo,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi,
    maxPerPage: MaxPerPage
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers.{ NotificationBSONHandler, NotifiesHandler }
  import jsonHandlers._

  def getNotifications(userId: Notification.Notifies, page: Int): Fu[Paginator[Notification]] =
    Paginator(
      adapter = new Adapter(
        collection = repo.coll,
        selector = repo.userNotificationsQuery(userId),
        projection = none,
        sort = repo.recentSort
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def getNotificationsAndCount(userId: Notification.Notifies, page: Int): Fu[Notification.AndUnread] =
    getNotifications(userId, page) zip unreadCount(userId) dmap (Notification.AndUnread.apply _).tupled

  def markAllRead(userId: Notification.Notifies) =
    repo.markAllRead(userId) >>- unreadCountCache.put(userId, fuccess(0))

  def markAllRead(userIds: Iterable[Notification.Notifies]) =
    repo.markAllRead(userIds) >>- userIds.foreach {
      unreadCountCache.put(_, fuccess(0))
    }

  private val unreadCountCache = cacheApi[Notification.Notifies, Int](32768, "notify.unreadCountCache") {
    _.expireAfterAccess(20 minutes)
      .buildAsyncFuture(repo.unreadNotificationsCount)
  }

  def unreadCount(userId: Notification.Notifies): Fu[Notification.UnreadCount] =
    unreadCountCache get userId dmap Notification.UnreadCount.apply

  def addNotification(notification: Notification): Funit =
    // Add to database and then notify any connected clients of the new notification
    insertOrDiscardNotification(notification) map {
      _ foreach { notif =>
        notifyUser(notif.notifies)
      }
    }

  def addNotificationWithoutSkipOrEvent(notification: Notification): Funit =
    repo.insert(notification) >>- unreadCountCache.update(notification.notifies, _ + 1)

  def addNotifications(notifications: List[Notification]): Funit =
    notifications.map(addNotification).sequenceFu.void

  def remove(notifies: Notification.Notifies, selector: Bdoc): Funit =
    repo.remove(notifies, selector) >>- unreadCountCache.invalidate(notifies)

  def markRead(notifies: Notification.Notifies, selector: Bdoc): Funit =
    repo.markManyRead(selector ++ $doc("notifies" -> notifies, "read" -> false)) >>-
      unreadCountCache.invalidate(notifies)

  def exists = repo.exists _

  private def shouldSkip(notification: Notification) =
    (!notification.isMsg ?? userRepo.isKid(notification.notifies.value)) >>| {
      notification.content match {
        case MentionedInThread(_, _, topicId, _, _) =>
          repo.hasRecentNotificationsInThread(notification.notifies, topicId)
        case InvitedToStudy(_, _, studyId) => repo.hasRecentStudyInvitation(notification.notifies, studyId)
        case PrivateMessage(sender, _)     => repo.hasRecentPrivateMessageFrom(notification.notifies, sender)
        case _                             => fuFalse
      }
    }

  /** Inserts notification into the repository.
    *
    * If the user already has an unread notification on the topic, discard it.
    *
    * If the user does not already have an unread notification on the topic, returns it unmodified.
    */
  private def insertOrDiscardNotification(notification: Notification): Fu[Option[Notification]] =
    shouldSkip(notification) flatMap {
      case true  => fuccess(none)
      case false => addNotificationWithoutSkipOrEvent(notification) inject notification.some
    }

  private def notifyUser(notifies: Notification.Notifies): Funit =
    getNotificationsAndCount(notifies, 1) map { msg =>
      import play.api.libs.json.Json
      Bus.publish(
        SendTo.async(
          notifies.value,
          "notifications",
          () => {
            userRepo langOf notifies.value map I18nLangPicker.byStrOrDefault map { implicit lang =>
              jsonHandlers(msg)
            }
          }
        ),
        "socketUsers"
      )
    }
}
