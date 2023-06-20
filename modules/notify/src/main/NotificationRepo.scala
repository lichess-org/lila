package lila.notify

import reactivemongo.api.bson.ElementProducer

import lila.db.dsl.{ *, given }

final private class NotificationRepo(colls: NotifyColls)(using Executor):

  import BSONHandlers.given
  import Notification.UnreadCount

  private val coll = colls.notif

  def insert(notification: Notification) =
    coll.insert.one(notification).void

  def insertMany(notifications: Iterable[Notification]): Funit =
    coll.insert.many(notifications).void

  def remove(notifies: UserId, selector: Bdoc): Funit =
    coll.delete.one(userNotificationsQuery(notifies) ++ selector).void

  def markAllRead(notifies: UserId): Funit =
    markManyRead(unreadOnlyQuery(notifies))

  def markAllRead(notifies: Iterable[UserId]): Funit =
    markManyRead(unreadOnlyQuery(notifies))

  def markManyRead(doc: Bdoc): Funit =
    coll.update.one(doc, $set("read" -> true), multi = true).void

  def unreadNotificationsCount(userId: UserId): Fu[UnreadCount] =
    UnreadCount from coll.countSel(unreadOnlyQuery(userId))

  def hasRecent(note: Notification, criteria: ElementProducer, unreadSince: Duration): Fu[Boolean] =
    hasFresh(note.notifies, note.content.key, criteria, matchRecentOrUnreadSince(unreadSince))

  def hasRecentPrivateMessageFrom(to: UserId, from: UserId): Fu[Boolean] =
    hasFresh(to, tpe = "privateMessage", criteria = "content.user" -> from.value, matchUnreadSince(3.days))

  private def matchSince(since: Duration) =
    $doc("createdAt" $gt nowInstant.minus(since))

  private def matchUnreadSince(unreadSince: Duration) =
    $doc("read" -> false, "createdAt" $gt nowInstant.minus(unreadSince))

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

  val recentSort = $sort desc "createdAt"

  def userNotificationsQuery(userId: UserId) = $doc("notifies" -> userId)

  private def unreadOnlyQuery(userId: UserId) = $doc("notifies" -> userId, "read" -> false)

  private def unreadOnlyQuery(userIds: Iterable[UserId]) =
    $doc("notifies" $in userIds, "read" -> false)
