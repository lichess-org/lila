package lila.notify

import play.api.libs.json.Json
import scala.concurrent.duration.*
import scala.concurrent.Future

import lila.common.Bus
import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.hub.actorApi.socket.{ SendTo, SendTos }
import lila.memo.CacheApi.*
import lila.pref.{ Allows, NotifyAllows }
import lila.user.{ User, UserRepo }
import lila.i18n.*

final class NotifyApi(
    jsonHandlers: JSONHandlers,
    repo: NotificationRepo,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi,
    maxPerPage: MaxPerPage,
    prefApi: lila.pref.PrefApi,
    getLightUser: lila.common.LightUser.Getter
)(using scala.concurrent.ExecutionContext):

  import Notification.*
  import BSONHandlers.given
  import jsonHandlers.*

  private val unreadCountCache = cacheApi[UserId, Int](32768, "notify.unreadCountCache") {
    _.expireAfterAccess(15 minutes)
      .buildAsyncFuture(repo.unreadNotificationsCount)
  }

  def getNotifications(userId: UserId, page: Int): Fu[Paginator[Notification]] =
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

  def getNotificationsAndCount(userId: UserId, page: Int): Fu[Notification.AndUnread] =
    getNotifications(userId, page) zip unreadCount(userId) map (AndUnread.apply _).tupled

  def markAllRead(userId: UserId): Funit =
    repo.markAllRead(userId) >>- unreadCountCache.put(userId, fuccess(0))

  def markAllRead(userIds: Iterable[UserId]): Funit =
    repo.markAllRead(userIds) >>- userIds.foreach {
      unreadCountCache.put(_, fuccess(0))
    }

  def unreadCount(userId: UserId): Fu[Int] =
    unreadCountCache get userId

  def insertNotification(notification: Notification): Funit =
    repo.insert(notification) >>- unreadCountCache.update(notification.to, _ + 1)

  def remove(to: UserId, selector: Bdoc = $empty): Funit =
    repo.remove(to, selector) >>- unreadCountCache.invalidate(to)

  def markRead(to: UserId, selector: Bdoc): Funit =
    repo.markManyRead(selector ++ $doc("notifies" -> to, "read" -> false)) >>-
      unreadCountCache.invalidate(to)

  def exists = repo.exists

  def notifyOne(to: UserId, content: NotificationContent): Funit =
    val note = Notification.make(to, content)
    !shouldSkip(note) ifThen {
      insertNotification(note) >> {
        if (!Allows.canFilter(note.content.key)) bellOne(note.to)
        else
          prefApi.getNotificationPref(note.to) map (_ allows note.content.key) flatMap { allows =>
            allows.bell ?? bellOne(note.to)
            allows.push ?? fuccess(pushOne(NotifyAllows(note.to, allows), note.content))
          }
      }
    }

  // notifyMany tells clients that an update is available to bump their bell. there's no need
  // to assemble full notification pages for all clients at once, let them initiate
  def notifyMany(userIds: Iterable[UserId], content: NotificationContent): Funit =
    prefApi.getNotifyAllows(userIds, content.key) flatMap { recips =>
      pushMany(recips filter (_.allows.push), content)
      bellMany(recips, content)
    }

  private def bellOne(to: UserId): Funit =
    getNotifications(to, 1) zip unreadCount(to) dmap (AndUnread.apply _).tupled map { msg =>
      Bus.publish(
        SendTo.async(
          to,
          "notifications",
          () =>
            (userRepo langOf to) map I18nLangPicker.byStrOrDefault map (lang => jsonHandlers(msg)(using lang))
        ),
        "socketUsers"
      )
    }

  private def bellMany(recips: Iterable[NotifyAllows], content: NotificationContent) =
    val bells = recips filter (_.allows.bell) map (_.userId)
    bells map unreadCountCache.invalidate // or maybe update only if getIfPresent?
    repo.insertMany(bells map (to => Notification.make(to, content))) >>- {
      Bus.publish(
        SendTos(
          bells toSet,
          "notifications",
          Json.obj("incrementUnread" -> true)
        ),
        "socketUsers"
      )
    }

  private def pushOne(to: NotifyAllows, content: NotificationContent) =
    pushMany(Seq(to), content)

  private def pushMany(recips: Iterable[NotifyAllows], content: NotificationContent) =
    Bus.publish(PushNotification(recips, content), "notifyPush")

  private def shouldSkip(note: Notification): Fu[Boolean] =
    note.content match
      case MentionedInThread(_, _, topicId, _, _) =>
        userRepo.isKid(note.to) >>|
          repo.hasRecent(note, "content.topicId" -> topicId, 3.days)
      case InvitedToStudy(_, _, studyId) =>
        userRepo.isKid(note.to) >>|
          repo.hasRecent(note, "content.studyId" -> studyId, 3.days)
      case PrivateMessage(sender, _) =>
        repo.hasRecentPrivateMessageFrom(note.to, sender)
      case _ => userRepo.isKid(note.to)
    
