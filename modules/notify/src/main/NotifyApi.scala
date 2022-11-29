package lila.notify

import scala.concurrent.duration.*
import lila.common.Bus
import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.hub.actorApi.socket.SendTo
import lila.memo.CacheApi.*
import lila.user.UserRepo
import lila.i18n.I18nLangPicker

final class NotifyApi(
    jsonHandlers: JSONHandlers,
    repo: NotificationRepo,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi,
    maxPerPage: MaxPerPage
)(using scala.concurrent.ExecutionContext):

  import Notification.*
  import BSONHandlers.given
  import jsonHandlers.*

  def getNotifications(userId: Notifies, page: Int): Fu[Paginator[Notification]] =
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

  def getNotificationsAndCount(userId: Notifies, page: Int): Fu[AndUnread] =
    getNotifications(userId, page) zip unreadCount(userId) dmap (AndUnread.apply).tupled

  def markAllRead(userId: Notifies) =
    repo.markAllRead(userId) >>- unreadCountCache.put(userId, fuccess(UnreadCount(0)))

  def markAllRead(userIds: Iterable[Notifies]) =
    repo.markAllRead(userIds) >>- userIds.foreach {
      unreadCountCache.put(_, fuccess(UnreadCount(0)))
    }

  private val unreadCountCache =
    cacheApi[Notifies, UnreadCount](32768, "notify.unreadCountCache") {
      _.expireAfterAccess(15 minutes)
        .buildAsyncFuture(repo.unreadNotificationsCount)
    }

  def unreadCount(userId: Notifies): Fu[UnreadCount] =
    unreadCountCache get userId

  def addNotification(notification: Notification): Fu[Boolean] =
    // Add to database and then notify any connected clients of the new notification
    insertOrDiscardNotification(notification) map {
      case Some(note) =>
        notifyUser(note.notifies)
        true
      case None => false
    }

  def addNotificationWithoutSkipOrEvent(notification: Notification): Funit =
    repo.insert(notification) >>- unreadCountCache.update(notification.notifies, _ + 1)

  def addNotifications(notifications: List[Notification]): Funit =
    notifications.map(addNotification).sequenceFu.void

  def remove(notifies: Notifies, selector: Bdoc = $empty): Funit =
    repo.remove(notifies, selector) >>- unreadCountCache.invalidate(notifies)

  def markRead(notifies: Notifies, selector: Bdoc): Funit =
    repo.markManyRead(selector ++ $doc("notifies" -> notifies, "read" -> false)) >>-
      unreadCountCache.invalidate(notifies)

  def exists = repo.exists

  private def shouldSkip(notification: Notification) =
    (!notification.isMsg ?? userRepo.isKid(notification.notifies)) >>| {
      notification.content match
        case MentionedInThread(_, _, topicId, _, _) =>
          repo.hasRecentNotificationsInThread(notification.notifies, topicId)
        case InvitedToStudy(_, _, studyId) => repo.hasRecentStudyInvitation(notification.notifies, studyId)
        case PrivateMessage(sender, _)     => repo.hasRecentPrivateMessageFrom(notification.notifies, sender)
        case _                             => fuFalse
    }

  /** Inserts notification into the repository. If the user already has an unread notification on the topic,
    * discard it. If the user does not already have an unread notification on the topic, returns it
    * unmodified.
    */
  private def insertOrDiscardNotification(notification: Notification): Fu[Option[Notification]] =
    !shouldSkip(notification) flatMap {
      case true  => addNotificationWithoutSkipOrEvent(notification) inject notification.some
      case false => fuccess(None)
    }

  private def notifyUser(notifies: Notifies): Funit =
    getNotificationsAndCount(notifies, 1) map { msg =>
      Bus.publish(
        SendTo.async(
          notifies.id,
          "notifications",
          () => {
            userRepo langOf notifies.id map I18nLangPicker.byStrOrDefault map { lang =>
              jsonHandlers(msg)(using lang)
            }
          }
        ),
        "socketUsers"
      )
    }
