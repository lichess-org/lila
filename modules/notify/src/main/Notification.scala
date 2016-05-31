package lila.notify

import lila.notify.MentionedInThread.PostId
import org.joda.time.DateTime
import ornicar.scalalib.Random

case class NewNotification(notification: Notification, unreadNotifications: Int)

case class Notification(
    _id: String,
    notifies: Notification.Notifies,
    content: NotificationContent,
    read: Notification.NotificationRead,
    createdAt: DateTime) {
  def id = _id

  def unread = !read.value
}

object Notification {

  case class Notifies(value: String) extends AnyVal with StringValue
  case class NotificationRead(value: Boolean)

  def apply(notifies: Notification.Notifies, content: NotificationContent, read: NotificationRead, createdAt: DateTime): Notification = {
    val idSize = 8
    val id = Random nextStringUppercase idSize
    new Notification(id, notifies, content, read, createdAt)
  }
}

sealed trait NotificationContent
case class MentionedInThread(mentionedBy: MentionedInThread.MentionedBy,
  topic: MentionedInThread.Topic,
  topidId: MentionedInThread.TopicId,
  category: MentionedInThread.Category,
  postId: PostId) extends NotificationContent

object MentionedInThread {
  case class MentionedBy(value: String) extends AnyVal with StringValue
  case class Topic(value: String) extends AnyVal with StringValue
  case class TopicId(value: String) extends AnyVal with StringValue
  case class Category(value: String) extends AnyVal with StringValue
  case class PostId(value: String) extends AnyVal with StringValue
}

case class InvitedToStudy(invitedBy: InvitedToStudy.InvitedBy,
  studyName: InvitedToStudy.StudyName,
  studyId: InvitedToStudy.StudyId) extends NotificationContent

object InvitedToStudy {
  case class InvitedBy(value: String) extends AnyVal with StringValue
  case class StudyName(value: String) extends AnyVal with StringValue
  case class StudyId(value: String) extends AnyVal with StringValue
}
