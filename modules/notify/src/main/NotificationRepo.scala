package lila.notify

import org.joda.time.DateTime
import reactivemongo.api.bson.ElementProducer
import scala.concurrent.duration.{ Duration, DurationInt }

import lila.db.dsl._
import lila.user.User

final private class NotificationRepo(
    val coll: Coll,
    val userRepo: lila.user.UserRepo,
    val prefApi: lila.pref.PrefApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def insert(notification: Notification) =
    coll.insert.one(notification).void

  def insertMany(notifications: Iterable[Notification]): Funit =
    coll.insert.many(notifications).void

  def remove(notifies: User.ID, selector: Bdoc): Funit =
    coll.delete.one(userNotificationsQuery(notifies) ++ selector).void

  def markAllRead(notifies: User.ID): Funit =
    markManyRead(unreadOnlyQuery(notifies))

  def markAllRead(notifies: Iterable[User.ID]): Funit =
    markManyRead(unreadOnlyQuery(notifies))

  def markManyRead(doc: Bdoc): Funit =
    coll.update.one(doc, $set("read" -> true), multi = true).void

  def unreadNotificationsCount(userId: User.ID): Fu[Int] =
    coll.countSel(unreadOnlyQuery(userId))

  def hasRecent(note: Notification, criteria: ElementProducer, unreadSince: Duration): Fu[Boolean] =
    hasFresh(note.to, note.content.key, criteria, matchRecentOrUnreadSince(unreadSince))

  def hasRecentPrivateMessageFrom(to: User.ID, from: String): Fu[Boolean] =
    hasFresh(to, tpe = "privateMessage", criteria = "content.user" -> from, matchUnreadSince(3.days))

  private def matchSince(since: Duration) =
    $doc("createdAt" $gt DateTime.now.minus(since.toMillis))

  private def matchUnreadSince(unreadSince: Duration) =
    $doc("read" -> false, "createdAt" $gt DateTime.now.minus(unreadSince.toMillis))

  private def matchRecentOrUnreadSince(since: Duration) =
    $or(matchSince(10.minutes), matchUnreadSince(since))

  private def hasFresh(
      to: User.ID,
      tpe: String,
      criteria: ElementProducer,
      freshnessSelector: Bdoc
  ): Fu[Boolean] =
    coll.exists($doc("notifies" -> to, "content.type" -> tpe, criteria) ++ freshnessSelector)

  def exists(notifies: User.ID, selector: Bdoc): Fu[Boolean] =
    coll.exists(userNotificationsQuery(notifies) ++ selector)

  val recentSort = $sort desc "createdAt"

  def mostRecentUnread(userId: User.ID) =
    coll.find($doc("notifies" -> userId, "read" -> false)).sort($sort.createdDesc).one[Notification]

  def userNotificationsQuery(userId: User.ID) = $doc("notifies" -> userId)

  private def unreadOnlyQuery(userId: User.ID) = $doc("notifies" -> userId, "read" -> false)
  private def unreadOnlyQuery(userIds: Iterable[User.ID]) =
    $doc("notifies" $in userIds, "read" -> false)
}
