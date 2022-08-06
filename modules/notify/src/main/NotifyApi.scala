package lila.notify

import scala.concurrent.duration._
import scala.concurrent.Future
import lila.common.Bus
import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.hub.actorApi.socket.{SendTo, SendTos}
import lila.memo.CacheApi._
import lila.user.UserRepo
import lila.i18n._

final class NotifyApi(
    jsonHandlers: JSONHandlers,
    repo: NotificationRepo,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi,
    streamStarter: StreamStartHelper,
    maxPerPage: MaxPerPage,
    prefApi : lila.pref.PrefApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers.{ NotificationBSONHandler, NotifiesHandler }
  import Notification._

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

  def getNotificationsAndCount(userId: Notifies, page: Int): Fu[Notification.AndUnread] =
    getNotifications(userId, page) zip unreadCount(userId) dmap (Notification.AndUnread.apply _).tupled

  def markAllRead(userId: Notifies) =
    repo.markAllRead(userId) >>- unreadCountCache.put(userId, fuccess(0))

  def markAllRead(userIds: Iterable[Notifies]) =
    repo.markAllRead(userIds) >>- userIds.foreach {
      unreadCountCache.put(_, fuccess(0))
    }

  private val unreadCountCache = cacheApi[Notifies, Int](32768, "notify.unreadCountCache") {
    _.expireAfterAccess(15 minutes)
      .buildAsyncFuture(repo.unreadNotificationsCount)
  }

  def unreadCount(userId: Notifies): Fu[UnreadCount] =
    unreadCountCache get userId dmap UnreadCount.apply

  def addNotification(notification: Notification): Funit =
    // Add to database and then notify any connected clients of the new notification
    insertOrDiscardNotification(notification) map {
      _ foreach { notif =>
        notifyUser(notif)
      }
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

  def exists = repo.exists _

  /*
  look up the followers, filter by notification pref, push web/firebase if necessary,
  and assemble a NotiflowerView (see StreamStartHelper) where the text is translated
  for all recipients. bulk lookup the unread counts for users and update the unread cache.
  group notes by unique (lang, unreadCount, alert) tuples to determine the user set for SendTos.
  each SendTos message contains a single notification (no pager) with the translated text,
  streamer name/id, unread count, and either true/false whether to use Notification API if
  backgrounded (sigh..  yes we further group SendTos based on whether the user set has push
  enabled on streamStart)

  meanwhile, the typescript has been modified to handle these slimmed down messages
  (see ui/notify/_.ts).  a full page of notifications won't be delivered until they
  actually click the bell.  there is no per-user db access in this entire flow and
  it's streamlined wrt lila-ws tells.  magnus, please stay retired.
   */

  def notifyStreamStart(streamerId: String, streamerName: String): Funit =
    streamStarter.getNotiflowersAndPush(streamerId, streamerName) flatMap { res =>
      val views = streamStarter.NotiflowerView(res, streamerId, streamerName)

      repo.bulkUnreadCount(views.users) flatMap { countList => // count before insert is intentional
        bumpCountCache(countList filter (recents => views.byUser(recents._1).recentlyOnline))

        repo.insertMany(views.noteList) andThen { case _ =>
          views.notesByLang map { case (lang, sameLangNotes) =>
            notesByCount(sameLangNotes, countList.toMap) map { case (count, (users, note)) =>
              val (alertUsers, obliviousUsers) = views.alertPartition(users)
              sendTos(alertUsers, note, count, true, lang)
              sendTos(obliviousUsers, note, count, false, lang)
              // at this point, terrified to look at lila-ws.  what if grouping these msgs
              // into identical sendTos doesn't optimize traffic compared to many single sendTo
              // calls? (it kind of snowballed on me)...
            }
          }
        }
      }
    }

  private def sendTos(users: Set[String], note: Notification, count: Int, alert: Boolean, lang: play.api.i18n.Lang) =
    Bus.publish(
      SendTos(
        users,
        "notifications",
        jsonHandlers(Notification.SingleAndUnread(note, UnreadCount(count + 1), alert = alert))(lang)
      ),
      "socketUsers"
    )

  private def notesByCount(
      notes: Iterable[Option[Notification]],
      countMap: Map[String, Int]
  ) =
    notes
      .collect { case Some(note) =>
        (countMap.getOrElse(note.notifies.value, 0), note) // (count, note) tuple list
      }
      .groupMapReduce(_._1)                     // group list by count
      { t => (Set(t._2.notifies.value), t._2) } // wrap note.userId in set
      { (a, b) => (a._1 | b._1, a._2) }         // join userId sets and discard identical note

  private def bumpCountCache(countList: List[(String, Int)]) =
    countList map { case (userId, veryRecentCount) =>
      // we have the accurate count.  and everyone in countList is recently online
      unreadCountCache.put(Notifies(userId), Future({ veryRecentCount + 1 })) // +1 for the streamStart
    }

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

  /** Inserts notification into the repository. If the user already has an unread notification on the topic,
    * discard it. If the user does not already have an unread notification on the topic, returns it
    * unmodified.
    */
  private def insertOrDiscardNotification(notification: Notification): Fu[Option[Notification]] =
    !shouldSkip(notification) flatMap {
      _ ?? addNotificationWithoutSkipOrEvent(notification) inject notification.some
    }

  private def notifyUser(note: Notification) =
      getAllows(note.notifies.value, note) zip unreadCountCache.get(note.notifies) collect {
        case (allows, count) =>
          if (allows.push) pushToUser(note)
          if (allows.bell) Bus.publish(
            SendTo.async(
              note.notifies.value,
              "notifications",
              () => {
                userRepo langOf note.notifies.value map I18nLangPicker.byStrOrDefault map { implicit lang =>
                  jsonHandlers(SingleAndUnread(note, UnreadCount(count), allows.web))
                }
              }
            ),
            "socketUsers"
          )
      }

  private def pushToUser(note: Notification) = {
    note.content match {
      case MentionedInThread(commenter, topic, _, _, postId) =>
        userRepo.byId(note.notifies.value) collect { case Some(user) =>
          implicit val lang: play.api.i18n.Lang = user.realLang.getOrElse(defaultLang)
          Bus.publish(
            lila.hub.actorApi.push.ForumMention(
              commenter.value,
              I18nKeys.xMentionedYouInY.txt(commenter, topic),
              postId.value
            ),
            "forumMention"
          )
        }
      case _ =>
    }
  }

  import lila.pref.NotificationPref._
  private def getAllows(uid: String, note: Notification): Fu[Allows] =
    prefApi.getNotificationPref(uid) map { pref =>
      note.content match {
        case _: PrivateMessage => pref.allows(InboxMsg)
        case _: MentionedInThread => pref.allows(ForumMention)
        case _ => Allows(BELL)
      }
    }
}
