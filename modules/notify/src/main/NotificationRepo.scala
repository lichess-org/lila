package lila.notify

import lila.db.dsl._
import org.joda.time.DateTime

private final class NotificationRepo(val coll: Coll) {

  import BSONHandlers._

  def insert(notification: Notification) = {
    coll.insert(notification).void
  }

  def markAllRead(notifies: Notification.Notifies) : Funit = {
    coll.update(unreadOnlyQuery(notifies), $set("read" -> true), multi=true).void
  }

  def unreadNotificationsCount(userId: Notification.Notifies) : Fu[Int] = {
    coll.count(unreadOnlyQuery(userId).some)
  }

  def hasRecentUnseenStudyInvitation(userId: Notification.Notifies, studyId: InvitedToStudy.StudyId) : Fu[Boolean] = {
    val query = $doc(
      "notifies" -> userId,
      "read" -> false,
      "content.type" -> "invitedStudy",
      "content.studyId" -> studyId,
      "created" -> $doc("$gt" ->DateTime.now.minusDays(7))
    )

    coll.exists(query)
  }

  def hasRecentUnseenNotifcationsInThread(userId: Notification.Notifies, topicId: MentionedInThread.TopicId) : Fu[Boolean] = {
    val query = $doc(
      "notifies" -> userId,
      "read" -> false,
      "content.type" -> "mention",
      "content.topicId" -> topicId,
      "created" -> $doc("$gt" ->DateTime.now.minusDays(7))
    )

    coll.exists(query)
  }

  val recentSort = $sort desc "created"

  def userNotificationsQuery(userId: Notification.Notifies) = $doc("notifies" -> userId)

  private def unreadOnlyQuery(userId:Notification.Notifies) = $doc("notifies" -> userId, "read" -> false)

}