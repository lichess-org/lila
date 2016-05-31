package lila.notify

import lila.db.dsl._
import org.joda.time.DateTime

private final class NotificationRepo(val coll: Coll) {

  import BSONHandlers._

  def insert(notification: Notification) = {
    coll.insert(notification).void
  }

  def markAllRead(notifies: Notification.Notifies): Funit = {
    coll.update(unreadOnlyQuery(notifies), $set("read" -> true), multi = true).void
  }

  def unreadNotificationsCount(userId: Notification.Notifies): Fu[Int] = {
    coll.count(unreadOnlyQuery(userId).some)
  }

  private val recentSelector =
    $doc("$or" -> List(
      $doc( // old, unread
        "read" -> false,
        "createdAt" -> $doc("$gt" -> DateTime.now.minusDays(3))),
      $doc( // recent, read
        "createdAt" -> $doc("$gt" -> DateTime.now.minusMinutes(10))
      )))

  def hasRecentStudyInvitation(userId: Notification.Notifies, studyId: InvitedToStudy.StudyId): Fu[Boolean] =
    coll.exists($doc(
      "notifies" -> userId,
      "content.type" -> "invitedStudy",
      "content.studyId" -> studyId
    ) ++ recentSelector)

  def hasRecentNotificationsInThread(userId: Notification.Notifies, topicId: MentionedInThread.TopicId): Fu[Boolean] =
    coll.exists($doc(
      "notifies" -> userId,
      "content.type" -> "mention",
      "content.topicId" -> topicId
    ) ++ recentSelector)

  val recentSort = $sort desc "createdAt"

  def userNotificationsQuery(userId: Notification.Notifies) = $doc("notifies" -> userId)

  private def unreadOnlyQuery(userId: Notification.Notifies) = $doc("notifies" -> userId, "read" -> false)

}
