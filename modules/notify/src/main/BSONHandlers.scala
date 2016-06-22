package lila.notify

import chess.Color
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl._
import lila.db.{ dsl, BSON }
import lila.notify.InvitedToStudy.{ StudyName, InvitedBy, StudyId }
import lila.notify.MentionedInThread._
import lila.notify.Notification._
import reactivemongo.bson._

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
  implicit val QuestionHandler = Macros.handler[Question]
  implicit val AnswerIdHandler = intAnyValHandler[AnswerId](_.value, AnswerId.apply)
  implicit val QaAnswerHandler = Macros.handler[QaAnswer]

  implicit val TeamIdHandler = stringAnyValHandler[TeamJoined.Id](_.value, TeamJoined.Id.apply)
  implicit val TeamNameHandler = stringAnyValHandler[TeamJoined.Name](_.value, TeamJoined.Name.apply)
  implicit val TeamJoinedHandler = Macros.handler[TeamJoined]

  implicit val BlogIdHandler = stringAnyValHandler[NewBlogPost.Id](_.value, NewBlogPost.Id.apply)
  implicit val BlogSlugHandler = stringAnyValHandler[NewBlogPost.Slug](_.value, NewBlogPost.Slug.apply)
  implicit val BlogTitleHandler = stringAnyValHandler[NewBlogPost.Title](_.value, NewBlogPost.Title.apply)
  implicit val NewBlogPostHandler = Macros.handler[NewBlogPost]

  implicit val ColorBSONHandler = new BSONHandler[BSONBoolean, chess.Color] {
    def read(b: BSONBoolean) = chess.Color(b.value)
    def write(c: chess.Color) = BSONBoolean(c.white)
  }

  implicit val NotificationContentHandler = new BSON[NotificationContent] {

    private def writeNotificationType(notificationContent: NotificationContent) = {
      notificationContent match {
        case _: MentionedInThread        => "mention"
        case _: InvitedToStudy           => "invitedStudy"
        case _: PrivateMessage           => "privateMessage"
        case _: QaAnswer                 => "qaAnswer"
        case _: TeamJoined               => "teamJoined"
        case _: NewBlogPost              => "newBlogPost"
        case LimitedTournamentInvitation => "u"
      }
    }

    private def writeNotificationContent(notificationContent: NotificationContent) = {
      notificationContent match {
        case MentionedInThread(mentionedBy, topic, topicId, category, postId) =>
          $doc("mentionedBy" -> mentionedBy, "topic" -> topic, "topicId" -> topicId, "category" -> category, "postId" -> postId)
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          $doc("invitedBy" -> invitedBy, "studyName" -> studyName, "studyId" -> studyId)
        case p: PrivateMessage           => PrivateMessageHandler.write(p)
        case q: QaAnswer                 => QaAnswerHandler.write(q)
        case t: TeamJoined               => TeamJoinedHandler.write(t)
        case b: NewBlogPost              => NewBlogPostHandler.write(b)
        case LimitedTournamentInvitation => $empty
      }
    } ++ $doc("type" -> writeNotificationType(notificationContent))

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
      case "mention"          => readMentionedNotification(reader)
      case "invitedStudy"     => readInvitedStudyNotification(reader)
      case "privateMessage"   => PrivateMessageHandler read reader.doc
      case "qaAnswer"         => QaAnswerHandler read reader.doc
      case "teamJoined"       => TeamJoinedHandler read reader.doc
      case "newBlogPost"      => NewBlogPostHandler read reader.doc
      case "u"                => LimitedTournamentInvitation
    }

    def writes(writer: Writer, n: NotificationContent): dsl.Bdoc = {
      writeNotificationContent(n)
    }
  }

  implicit val NotificationBSONHandler = Macros.handler[Notification]
}
