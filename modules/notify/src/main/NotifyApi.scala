package lila.notify

import play.api.libs.json.Json

import lila.common.Bus
import lila.common.config.MaxPerPage
import lila.common.paginator.Paginator
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.hub.actorApi.socket.{ SendTo, SendTos }
import lila.memo.CacheApi.*
import lila.user.{ User, UserRepo }
import lila.i18n.*

final class NotifyApi(
    jsonHandlers: JSONHandlers,
    repo: NotificationRepo,
    colls: NotifyColls,
    userRepo: UserRepo,
    cacheApi: lila.memo.CacheApi,
    maxPerPage: MaxPerPage
)(using Executor):

  import Notification.*
  import BSONHandlers.given
  import jsonHandlers.*

  object prefs:
    import NotificationPref.{ *, given }

    def form(me: User) =
      colls.pref
        .byId[NotificationPref](me.id)
        .dmap(_ | default)
        .map(NotificationPref.form.form.fill)

    def set(me: User, pref: NotificationPref) =
      colls.pref.update.one($id(me.id), pref, upsert = true).void

    def allows(userId: UserId, event: Event): Fu[Allows] =
      colls.pref
        .primitiveOne[Allows]($id(userId), event.key)
        .dmap(_ | default.allows(event))

    def getAllows(userIds: Iterable[UserId], event: NotificationPref.Event): Fu[List[NotifyAllows]] =
      colls.pref.tempPrimary
        .find($inIds(userIds), $doc(event.key -> true).some)
        .cursor[Bdoc]()
        .listAll()
        .map: docs =>
          val customAllows = for
            doc    <- docs
            userId <- doc.getAsOpt[UserId]("_id")
            allows <- doc.getAsOpt[Allows](event.key)
          yield NotifyAllows(userId, allows)
          val customIds = customAllows.view.map(_.userId).toSet
          val defaultAllows = userIds.filterNot(customIds.contains).map {
            NotifyAllows(_, NotificationPref.default.allows(event))
          }
          customAllows ::: defaultAllows.toList

  private val unreadCountCache = cacheApi[UserId, UnreadCount](32768, "notify.unreadCountCache") {
    _.expireAfterAccess(15 minutes)
      .buildAsyncFuture(repo.unreadNotificationsCount)
  }

  def getNotifications(userId: UserId, page: Int): Fu[Paginator[Notification]] =
    Paginator(
      adapter = new Adapter(
        collection = colls.notif,
        selector = repo.userNotificationsQuery(userId),
        projection = none,
        sort = repo.recentSort
      ),
      currentPage = page,
      maxPerPage = maxPerPage
    )

  def getNotificationsAndCount(userId: UserId, page: Int): Fu[Notification.AndUnread] =
    getNotifications(userId, page) zip unreadCount(userId) map AndUnread.apply

  def markAllRead(userId: UserId): Funit =
    repo.markAllRead(userId) andDo unreadCountCache.put(userId, fuccess(UnreadCount(0)))

  def markAllRead(userIds: Iterable[UserId]): Funit =
    repo.markAllRead(userIds) andDo userIds.foreach:
      unreadCountCache.put(_, fuccess(UnreadCount(0)))

  def unreadCount(userId: UserId): Fu[UnreadCount] =
    unreadCountCache get userId

  def insertNotification(notification: Notification): Funit =
    repo.insert(notification) andDo unreadCountCache.update(notification.to, _ + 1)

  def remove(to: UserId, selector: Bdoc = $empty): Funit =
    repo.remove(to, selector) andDo unreadCountCache.invalidate(to)

  def markRead(to: UserId, selector: Bdoc): Funit =
    repo.markManyRead(selector ++ $doc("notifies" -> to, "read" -> false)) andDo
      unreadCountCache.invalidate(to)

  def exists = repo.exists

  def notifyOne[U: UserIdOf](to: U, content: NotificationContent): Funit =
    val note = Notification.make(to, content)
    !shouldSkip(note) flatMapz {
      NotificationPref.Event.byKey.get(content.key) match
        case None => bellOne(note)
        case Some(event) =>
          prefs.allows(note.to, event) map { allows =>
            if allows.bell then bellOne(note)
            if allows.push then pushOne(NotifyAllows(note.to, allows), note.content)
          }
    }

  // notifyMany tells clients that an update is available to bump their bell. there's no need
  // to assemble full notification pages for all clients at once, let them initiate
  def notifyMany(userIds: Iterable[UserId], content: NotificationContent): Funit =
    NotificationPref.Event.byKey.get(content.key) so { event =>
      prefs.getAllows(userIds, event) flatMap { recips =>
        pushMany(recips.filter(_.allows.push), content)
        bellMany(recips, content)
      }
    }
  private[notify] def notifyManyIgnoringPrefs(userIds: Seq[UserId], content: NotificationContent): Funit =
    val recips = userIds.map(NotifyAllows(_, Allows.all))
    pushMany(recips, content)
    bellMany(recips, content)

  private def bellOne(note: Notification): Funit =
    insertNotification(note) andDo
      Bus.publish(
        SendTo.onlineUser(
          note.to,
          "notifications",
          () =>
            for
              notifications <- getNotifications(note.to, 1) zip unreadCount(note.to) dmap AndUnread.apply
              langStr       <- userRepo.langOf(note.to)
              lang = I18nLangPicker.byStrOrDefault(langStr)
            yield jsonHandlers(notifications)(using lang)
        ),
        "socketUsers"
      )

  private def bellMany(recips: Iterable[NotifyAllows], content: NotificationContent): Funit =
    val bells = recips.collect { case r if r.allows.bell => r.userId }
    bells foreach unreadCountCache.invalidate // or maybe update only if getIfPresent?
    repo.insertMany(bells.map(to => Notification.make(to, content))) andDo
      Bus.publish(
        SendTos(bells.toSet, "notifications", Json.obj("incrementUnread" -> true)),
        "socketUsers"
      )

  private def pushOne(to: NotifyAllows, content: NotificationContent) =
    pushMany(Seq(to), content)

  private def pushMany(recips: Iterable[NotifyAllows], content: NotificationContent) =
    Bus.publish(PushNotification(recips, content), "notifyPush")

  private def shouldSkip(note: Notification): Fu[Boolean] = note.content match
    case MentionedInThread(_, _, topicId, _, _) =>
      userRepo.isKid(note.to) >>|
        repo.hasRecent(note, "content.topicId" -> topicId, 3.days)
    case InvitedToStudy(_, _, studyId) =>
      userRepo.isKid(note.to) >>|
        repo.hasRecent(note, "content.studyId" -> studyId, 3.days)
    case PrivateMessage(sender, _) =>
      repo.hasRecentPrivateMessageFrom(note.to, sender)
    case _: CorresAlarm => fuFalse
    case _              => userRepo.isKid(note.to)
