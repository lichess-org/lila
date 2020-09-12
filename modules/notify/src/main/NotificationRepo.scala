package lila.notify

import lila.db.dsl._
import org.joda.time.DateTime

final private class NotificationRepo(val coll: Coll)(implicit ec: scala.concurrent.ExecutionContext) {

  import BSONHandlers._

  def insert(notification: Notification) =
    coll.insert.one(notification).void

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
      "createdAt" $gt DateTime.now.minusDays(3)
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
