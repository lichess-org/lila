package lila.notify

import org.joda.time.DateTime
import reactivemongo.api.ReadPreference

import lila.db.dsl._

final private class NotificationRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def insert(notification: Notification) =
    coll.insert.one(notification).void

  def insertMany(notifications: Iterable[Notification]): Funit =
    coll.insert.many(notifications).void

  def remove(notifies: Notification.Notifies, selector: Bdoc): Funit =
    coll.delete.one(userNotificationsQuery(notifies) ++ selector).void

  def markAllRead(notifies: Notification.Notifies): Funit =
    markManyRead(unreadOnlyQuery(notifies))

  def markAllRead(notifies: Iterable[Notification.Notifies]): Funit =
    markManyRead(unreadOnlyQuery(notifies))

  def markManyRead(doc: Bdoc): Funit =
    coll.update.one(doc, $set("read" -> true), multi = true).void

  def unreadNotificationsCount(userId: Notification.Notifies): Fu[Int] =
    coll.countSel(unreadOnlyQuery(userId))

  private def hasOld =
    $doc(
      "read" -> false,
      $or(
        $doc("content.type" -> $ne("streamStart"), "createdAt" $gt DateTime.now.minusDays(3)),
        $doc("content.type" -> "streamStart", "createdAt" $gt DateTime.now.minusHours(2))
      )
    )

  private def hasUnread =
    $doc( // recent, read
      "createdAt" $gt DateTime.now.minusMinutes(10)
    )

  private def hasOldOrUnread =
    $doc("$or" -> List(hasOld, hasUnread))

  def hasRecentStudyInvitation(userId: Notification.Notifies, studyId: InvitedToStudy.StudyId): Fu[Boolean] =
    coll.exists(
      $doc(
        "notifies"        -> userId,
        "content.type"    -> "invitedStudy",
        "content.studyId" -> studyId
      ) ++ hasOldOrUnread
    )

  def bulkUnreadCount(userIds: Iterable[String]): Fu[List[(String, Int)]] = {
    coll.aggregateList(-1, ReadPreference.secondaryPreferred) { f =>
      f.Match($doc("notifies" $in userIds) ++ hasOld) ->
        List(f.GroupField("notifies")("nb" -> f.SumAll))
    } map { docs =>
      for {
        doc   <- docs
        user  <- doc string "_id"
        count <- doc int "nb"
      } yield (user, count)
    }
  }

  def hasRecentNotificationsInThread(
      userId: Notification.Notifies,
      topicId: MentionedInThread.TopicId
  ): Fu[Boolean] =
    coll.exists(
      $doc(
        "notifies"        -> userId,
        "content.type"    -> "mention",
        "content.topicId" -> topicId
      ) ++ hasOldOrUnread
    )

  def hasRecentPrivateMessageFrom(
      userId: Notification.Notifies,
      sender: PrivateMessage.Sender
  ): Fu[Boolean] =
    coll.exists(
      $doc(
        "notifies"     -> userId,
        "content.type" -> "privateMessage",
        "content.user" -> sender
      ) ++ hasOld
    )

  def exists(notifies: Notification.Notifies, selector: Bdoc): Fu[Boolean] =
    coll.exists(userNotificationsQuery(notifies) ++ selector)

  val recentSort = $sort desc "createdAt"

  def userNotificationsQuery(userId: Notification.Notifies) = $doc("notifies" -> userId)

  private def unreadOnlyQuery(userId: Notification.Notifies) = $doc("notifies" -> userId, "read" -> false)
  private def unreadOnlyQuery(userIds: Iterable[Notification.Notifies]) =
    $doc("notifies" $in userIds, "read" -> false)

}
