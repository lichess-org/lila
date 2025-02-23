package lila.notify

import play.api.libs.json.Json

import lila.common.Bus

import scalalib.paginator.Paginator
import scalalib.data.LazyFu
import lila.db.dsl.{ *, given }
import lila.db.paginator.Adapter
import lila.core.socket.SendTos
import lila.memo.CacheApi.*
import lila.core.i18n.{ Translator, LangPicker }
import lila.core.notify.{ NotificationPref as _, * }
import lila.core.notify.NotificationContent.*
import lila.core.socket.SendToOnlineUser

final class NotifyApi(
    jsonHandlers: JSONHandlers,
    repo: NotificationRepo,
    colls: NotifyColls,
    userApi: lila.core.user.UserApi,
    cacheApi: lila.memo.CacheApi,
    maxPerPage: MaxPerPage,
    langPicker: LangPicker
)(using Executor, Translator)
    extends lila.core.notify.NotifyApi(colls.pref):

  import Notification.*
  import BSONHandlers.given
  import jsonHandlers.*

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    for
      _ <- colls.pref.delete.one($id(del.id))
      _ <- colls.notif.delete.one($doc("notifies" -> del.id))
    yield ()

  object prefs:
    import NotificationPref.{ *, given }

    def form[U: UserIdOf](me: U) =
      colls.pref
        .byId[NotificationPref](me.id)
        .dmap(_ | default)
        .map(NotificationPref.form.form.fill)

    def set[U: UserIdOf](me: U, pref: NotificationPref) =
      colls.pref.update.one($id(me.id), pref, upsert = true).void

    def allows(userId: UserId, event: PrefEvent): Fu[Allows] =
      colls.pref
        .primitiveOne[Allows]($id(userId), event.key)
        .dmap(_ | default.allows(event))

    def getAllows(userIds: Iterable[UserId], event: PrefEvent): Fu[List[NotifyAllows]] =
      userIds.nonEmpty.so:
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
            val defaultAllows = userIds
              .filterNot(customIds.contains)
              .map:
                NotifyAllows(_, NotificationPref.default.allows(event))
            customAllows ::: defaultAllows.toList

  private val unreadCountCache = cacheApi[UserId, UnreadCount](65_536, "notify.unreadCountCache"):
    _.expireAfterAccess(15.minutes).buildAsyncFuture(repo.expireAndCount)

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
    getNotifications(userId, page).zip(unreadCount(userId)).map(AndUnread.apply)

  def markAllRead(userId: UserId): Funit =
    for _ <- repo.markAllRead(userId)
    yield unreadCountCache.put(userId, fuccess(UnreadCount(0)))

  def markAllRead(userIds: Iterable[UserId]): Funit =
    for _ <- repo.markAllRead(userIds)
    yield userIds.foreach:
      unreadCountCache.put(_, fuccess(UnreadCount(0)))

  def unreadCount(userId: UserId): Fu[UnreadCount] =
    unreadCountCache.get(userId)

  def insertNotification(notification: Notification): Funit =
    for _ <- repo.insert(notification)
    yield unreadCountCache.update(notification.to, _ + 1)

  def remove(to: UserId, selector: Bdoc = $empty): Funit =
    for _ <- repo.remove(to, selector)
    yield unreadCountCache.invalidate(to)

  def markRead(to: UserId, selector: Bdoc): Funit =
    repo
      .markManyRead(selector ++ $doc("notifies" -> to, "read" -> false))
      .map: nb =>
        if nb > 0 then unreadCountCache.invalidate(to)

  def notifyOne[U: UserIdOf](to: U, content: NotificationContent): Funit =
    val note = Notification.make(to, content)
    shouldSkip(note).not.flatMapz:
      NotificationPref.events.get(content.key) match
        case None => bellOne(note)
        case Some(event) =>
          prefs.allows(note.to, event).map { allows =>
            if allows.bell then bellOne(note)
            if allows.push then pushOne(NotifyAllows(note.to, allows), note.content)
          }

  // notifyMany tells clients that an update is available to bump their bell. there's no need
  // to assemble full notification pages for all clients at once, let them initiate
  def notifyMany(userIds: Iterable[UserId], content: NotificationContent): Funit =
    NotificationPref.events
      .get(content.key)
      .so: event =>
        prefs
          .getAllows(userIds, event)
          .flatMap: recips =>
            pushMany(recips.filter(_.allows.push), content)
            bellMany(recips, content)

  private[notify] def notifyManyIgnoringPrefs(userIds: Seq[UserId], content: NotificationContent): Funit =
    val recips = userIds.map(NotifyAllows(_, lila.notify.Allows.all))
    pushMany(recips, content)
    bellMany(recips, content)

  private def bellOne(note: Notification): Funit =
    for _ <- insertNotification(note)
    yield Bus.publish(
      SendToOnlineUser(
        note.to,
        LazyFu: () =>
          for
            notifications <- getNotifications(note.to, 1).zip(unreadCount(note.to)).dmap(AndUnread.apply)
            langStr       <- userApi.langOf(note.to)
            lang = langPicker.byStrOrDefault(langStr)
          yield Json.obj(
            "t" -> "notifications",
            "d" -> jsonHandlers(notifications)(using summon[Translator].to(lang))
          )
      ),
      "socketUsers"
    )

  private def bellMany(recips: Iterable[NotifyAllows], content: NotificationContent): Funit =
    val expiresIn = content match
      case _: StreamStart    => 6.hours.some
      case _: BroadcastRound => 6.hours.some
      case _                 => none
    val bells = recips.collect { case r if r.allows.bell => r.userId }
    bells.foreach(unreadCountCache.invalidate) // or maybe update only if getIfPresent?
    for _ <- repo.insertMany(bells.map(to => Notification.make(to, content, expiresIn)))
    yield Bus.publish(
      SendTos(bells.toSet, "notifications", Json.obj("incrementUnread" -> true)),
      "socketUsers"
    )

  private def pushOne(to: NotifyAllows, content: NotificationContent) =
    pushMany(Seq(to), content)

  private def pushMany(recips: Iterable[NotifyAllows], content: NotificationContent) =
    Bus.publish(PushNotification(recips, content), "notifyPush")

  private def shouldSkip(note: Notification): Fu[Boolean] = note.content match
    case MentionedInThread(_, _, topicId, _, _) =>
      userApi.isKid(note.to) >>|
        repo.hasRecent(note, "content.topicId" -> topicId, 3.days)
    case InvitedToStudy(_, _, studyId) =>
      userApi.isKid(note.to) >>|
        repo.hasRecent(note, "content.studyId" -> studyId, 3.days)
    case PrivateMessage(sender, _) =>
      repo.hasRecentPrivateMessageFrom(note.to, sender)
    case _: CorresAlarm => fuFalse
    case _              => userApi.isKid(note.to)
