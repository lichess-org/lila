package lila.notify

import lila.common.LightUser
import play.api.libs.json._

final class JSONHandlers(
    getLightUser: LightUser.Getter) {

  implicit val notificationWrites: Writes[Notification] = new Writes[Notification] {
    def writeBody(notificationContent: NotificationContent) = {
      notificationContent match {
        case MentionedInThread(mentionedBy, topic, _, category, postId) =>
          Json.obj("mentionedBy" -> getLightUser(mentionedBy.value),
            "topic" -> topic.value, "category" -> category.value,
            "postId" -> postId.value)
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          Json.obj("invitedBy" -> getLightUser(invitedBy.value),
            "studyName" -> studyName.value,
            "studyId" -> studyId.value)
      }
    }

    def writes(notification: Notification) = {
      val body = notification.content

      val notificationType = body match {
        case MentionedInThread(_, _, _, _, _) => "mentioned"
        case InvitedToStudy(_, _, _)          => "invitedStudy"
      }

      Json.obj("content" -> writeBody(body),
        "type" -> notificationType,
        "read" -> notification.read.value,
        "date" -> notification.createdAt)
    }
  }

  import lila.common.paginator.PaginatorJson._
  implicit val unreadWrites = Writes[Notification.UnreadCount] { v => JsNumber(v.value) }
  implicit val andUnreadWrites: Writes[Notification.AndUnread] = Json.writes[Notification.AndUnread]

  implicit val newNotificationWrites: Writes[NewNotification] = new Writes[NewNotification] {

    def writes(newNotification: NewNotification) = {
      Json.obj("notification" -> newNotification.notification, "unread" -> newNotification.unreadNotifications)
    }
  }
}

