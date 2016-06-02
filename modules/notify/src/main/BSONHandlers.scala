package lila.notify

import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._
import lila.db.{ dsl, BSON }
import lila.notify.InvitedToStudy.{ StudyName, InvitedBy, StudyId }
import lila.notify.MentionedInThread._
import lila.notify.Notification._
import reactivemongo.bson.Macros
import reactivemongo.bson.{ BSONString, BSONHandler, BSONDocument }

private object BSONHandlers {

  implicit val NotifiesHandler = stringAnyValHandler[Notifies](_.value, Notifies.apply)

  implicit val MentionByHandler = stringAnyValHandler[MentionedBy](_.value, MentionedBy.apply)
  implicit val TopicHandler = stringAnyValHandler[Topic](_.value, Topic.apply)
  implicit val TopicIdHandler = stringAnyValHandler[TopicId](_.value, TopicId.apply)
  implicit val CategoryHandler = stringAnyValHandler[Category](_.value, Category.apply)
  implicit val PostIdHandler = stringAnyValHandler[PostId](_.value, PostId.apply)

  implicit val InvitedToStudyByHandler = stringAnyValHandler[InvitedBy](_.value, InvitedBy.apply)
  implicit val StudyNameHandler = stringAnyValHandler[StudyName](_.value, StudyName.apply)
  implicit val StudyIdHandler = stringAnyValHandler[StudyId](_.value, StudyId.apply)
  import Notification.{ Notifies, NotificationRead }
  implicit val ReadHandler = booleanAnyValHandler[NotificationRead](_.value, NotificationRead.apply)

  import PrivateMessage._
  implicit val PMThreadHandler = Macros.handler[Thread]
  implicit val PMSenderIdHandler = stringAnyValHandler[SenderId](_.value, SenderId.apply)
  implicit val PMTextHandler = stringAnyValHandler[Text](_.value, Text.apply)
  implicit val PrivateMessageHandler = Macros.handler[PrivateMessage]

  import QaAnswer._
  implicit val AnswererHandler = stringAnyValHandler[AnswererId](_.value, AnswererId.apply)
  implicit val TitleHandler = stringAnyValHandler[Title](_.value, Title.apply)
  implicit val QuestionIdHandler = intAnyValHandler[QuestionId](_.value, QuestionId.apply)
  implicit val QuestionSlugHandler = stringAnyValHandler[QuestionSlug](_.value, QuestionSlug.apply)
  implicit val AnswerIdHandler = intAnyValHandler[AnswerId](_.value, AnswerId.apply)
  implicit val QaAnswerHandler = Macros.handler[QaAnswer]

  implicit val NotificationContentHandler = new BSON[NotificationContent] {

    private def writeNotificationType(notificationContent: NotificationContent) = {
      notificationContent match {
        case _: MentionedInThread => "mention"
        case _: InvitedToStudy    => "invitedStudy"
        case _: PrivateMessage    => "privateMessage"
        case _: QaAnswer          => "qaAnswer"
      }
    }

    private def writeNotificationContent(notificationContent: NotificationContent) = {
      notificationContent match {
        case MentionedInThread(mentionedBy, topic, topicId, category, postId) =>
          $doc("type" -> writeNotificationType(notificationContent), "mentionedBy" -> mentionedBy,
            "topic" -> topic, "topicId" -> topicId, "category" -> category, "postId" -> postId)
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          $doc("type" -> writeNotificationType(notificationContent),
            "invitedBy" -> invitedBy,
            "studyName" -> studyName,
            "studyId" -> studyId)
        case p: PrivateMessage => PrivateMessageHandler.write(p) ++
          $doc("type" -> writeNotificationType(notificationContent))
        case q: QaAnswer => QaAnswerHandler.write(q) ++
          $doc("type" -> writeNotificationType(notificationContent))
      }
    }

    private def readMentionedNotification(reader: Reader): MentionedInThread = {
      val mentionedBy = reader.get[MentionedBy]("mentionedBy")
      val topic = reader.get[Topic]("topic")
      val topicId = reader.get[TopicId]("topicId")
      val category = reader.get[Category]("category")
      val postNumber = reader.get[PostId]("postId")

      MentionedInThread(mentionedBy, topic, topicId, category, postNumber)
    }

    private def readInvitedStudyNotification(reader: Reader): NotificationContent = {
      val invitedBy = reader.get[InvitedBy]("invitedBy")
      val studyName = reader.get[StudyName]("studyName")
      val studyId = reader.get[StudyId]("studyId")

      InvitedToStudy(invitedBy, studyName, studyId)
    }

    def reads(reader: Reader): NotificationContent = reader.str("type") match {
      case "mention"        => readMentionedNotification(reader)
      case "invitedStudy"   => readInvitedStudyNotification(reader)
      case "privateMessage" => PrivateMessageHandler read reader.doc
      case "qaAnswer"       => QaAnswerHandler read reader.doc
    }

    def writes(writer: Writer, n: NotificationContent): dsl.Bdoc = {
      writeNotificationContent(n)
    }
  }

  implicit val NotificationBSONHandler = Macros.handler[Notification]
}
