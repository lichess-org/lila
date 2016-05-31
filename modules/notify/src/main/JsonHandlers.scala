package lila.notify

import play.api.libs.json.{JsValue, Json, Writes}
import lila.common.LightUser
import lila.user.User


object JSONHandlers {

  implicit val notificationWrites : Writes[Notification] = new Writes[Notification] {
    def writeBody(notificationContent: NotificationContent) = {
      notificationContent match {
        case MentionedInThread(mentionedBy, topic, _, category, postId) =>
          Json.obj("mentionedBy" -> lila.user.Env.current.lightUser(mentionedBy.value),
            "topic" -> topic.value, "category" -> category.value,
            "postId" -> postId.value)
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          Json.obj("invitedBy" -> lila.user.Env.current.lightUser(invitedBy.value),
            "studyName" -> studyName.value,
            "studyId" -> studyId.value)
      }
    }

    def writes(notification: Notification) = {
        val body = notification.content

        val notificationType = body match {
          case MentionedInThread(_,_, _, _, _) => "mentioned"
          case InvitedToStudy(_,_,_) => "invitedStudy"
        }

        Json.obj("content" -> writeBody(body),
          "type" -> notificationType,
          "read" -> notification.read.value,
          "date" -> notification.createdAt)
    }
  }

  implicit val newNotificationWrites: Writes[NewNotification] = new Writes[NewNotification] {

    def writes(newNotification: NewNotification) = {
      Json.obj("notification" -> newNotification.notification, "unread" -> newNotification.unreadNotifications)
    }
  }
}


