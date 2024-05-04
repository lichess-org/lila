package lila.notify

import reactivemongo.api.bson.ElementProducer

import lila.db.dsl.{ *, given }
import lila.core.notify.UnreadCount

final private class NotificationRepo(colls: NotifyColls)(using Executor):

  import BSONHandlers.given

  private val coll = colls.notif

  def insert(notification: Notification) =
    coll.insert.one(notification).void

  def insertMany(notifications: Iterable[Notification]): Funit =
    coll.insert.many(notifications).void

  def remove(notifies: UserId, selector: Bdoc): Funit =
    coll.delete.one(userNotificationsQuery(notifies) ++ selector).void

  def markAllRead(notifies: UserId): Funit =
    markManyRead(unreadOnlyQuery(notifies)).void

  def markAllRead(notifies: Iterable[UserId]): Funit =
    markManyRead(unreadOnlyQuery(notifies)).void

  def markManyRead(selector: Bdoc): Fu[Int] =
    coll.update.one(selector, $set("read" -> true), multi = true).dmap(_.n)

  def expireAndCount(userId: UserId): Fu[UnreadCount] = for
    count   <- UnreadCount.from(coll.countSel(unreadOnlyQuery(userId)))
    expired <- (count > 0).so(markManyRead(expiredQuery(userId)))
  yield count - expired

  def hasRecent(note: Notification, criteria: ElementProducer, unreadSince: Duration): Fu[Boolean] =
    hasFresh(note.notifies, note.content.key, criteria, matchRecentOrUnreadSince(unreadSince))

  def hasRecentPrivateMessageFrom(to: UserId, from: UserId): Fu[Boolean] =
    hasFresh(to, tpe = "privateMessage", criteria = "content.user" -> from.value, matchUnreadSince(3.days))

  private def matchSince(since: Duration) =
    $doc("createdAt".$gt(nowInstant.minus(since)))

  private def matchUnreadSince(unreadSince: Duration) =
    $doc("read" -> false, "createdAt".$gt(nowInstant.minus(unreadSince)))

  private def matchRecentOrUnreadSince(since: Duration) =
    $or(matchSince(10.minutes), matchUnreadSince(since))

  private def hasFresh(
      to: UserId,
      tpe: String,
      criteria: ElementProducer,
      freshnessSelector: Bdoc
  ): Fu[Boolean] =
    coll.exists($doc("notifies" -> to, "content.type" -> tpe, criteria) ++ freshnessSelector)

  def exists(notifies: UserId, selector: Bdoc): Fu[Boolean] =
    coll.exists(userNotificationsQuery(notifies) ++ selector)

  val recentSort = $sort.desc("createdAt")

  def userNotificationsQuery(userId: UserId) = $doc("notifies" -> userId)

  private def unreadOnlyQuery(userId: UserId) = $doc("notifies" -> userId, "read" -> false)

  private def unreadOnlyQuery(userIds: Iterable[UserId]) =
    $doc("notifies".$in(userIds), "read" -> false)

  private def expiredQuery(userId: UserId) = unreadOnlyQuery(userId) ++ $doc("expiresAt".$lt(nowInstant))
