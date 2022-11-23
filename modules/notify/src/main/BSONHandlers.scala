package lila.notify

import chess.Color
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.{ *, given }
import lila.notify.InvitedToStudy.InvitedBy
import lila.notify.MentionedInThread.*
import lila.notify.Notification.*
import reactivemongo.api.bson.*

private object BSONHandlers:

  given BSONHandler[Notifies] = stringAnyValHandler[Notifies](_.value, Notifies.apply)

  given BSONHandler[MentionedBy] = stringAnyValHandler[MentionedBy](_.value, MentionedBy.apply)
  given BSONHandler[Topic]       = stringAnyValHandler[Topic](_.value, Topic.apply)
  given BSONHandler[TopicId]     = stringAnyValHandler[TopicId](_.value, TopicId.apply)
  given BSONHandler[Category]    = stringAnyValHandler[Category](_.value, Category.apply)
  given BSONHandler[PostId]      = stringAnyValHandler[PostId](_.value, PostId.apply)

  given BSONHandler[InvitedBy] = stringAnyValHandler[InvitedBy](_.value, InvitedBy.apply)
  given BSONHandler[NotificationRead] =
    booleanAnyValHandler[NotificationRead](_.value, NotificationRead.apply)

  import PrivateMessage.*
  given BSONHandler[Sender] = stringAnyValHandler[Sender](_.value, Sender.apply)
  given BSONHandler[Text]   = stringAnyValHandler[Text](_.value, Text.apply)
  private given PrivateMessageHandler: BSONDocumentHandler[PrivateMessage] = Macros.handler

  private given TeamJoinedHandler: BSONDocumentHandler[TeamJoined] = Macros.handler

  given BSONHandler[GameEnd.OpponentId] =
    stringAnyValHandler[GameEnd.OpponentId](_.value, GameEnd.OpponentId.apply)
  given BSONHandler[GameEnd.Win] = booleanAnyValHandler[GameEnd.Win](_.value, GameEnd.Win.apply)
  private given GameEndHandler: BSONDocumentHandler[GameEnd] = Macros.handler

  private given TitledTournamentInvitationHandler: BSONDocumentHandler[TitledTournamentInvitation] =
    Macros.handler

  private given PlanStartHandler: BSONDocumentHandler[PlanStart]   = Macros.handler
  private given PlanExpireHandler: BSONDocumentHandler[PlanExpire] = Macros.handler

  private given RatingRefundHandler: BSONDocumentHandler[RatingRefund] = Macros.handler
  private given CorresAlarmHandler: BSONDocumentHandler[CorresAlarm]   = Macros.handler
  private given IrwinDoneHandler: BSONDocumentHandler[IrwinDone]       = Macros.handler
  private given KaladinDoneHandler: BSONDocumentHandler[KaladinDone]   = Macros.handler
  private given GenericLinkHandler: BSONDocumentHandler[GenericLink]   = Macros.handler

  given lila.db.BSON[NotificationContent] with
    private def writeNotificationContent(notificationContent: NotificationContent) = {
      notificationContent match
        case MentionedInThread(mentionedBy, topic, topicId, category, postId) =>
          $doc(
            "mentionedBy" -> mentionedBy,
            "topic"       -> topic,
            "topicId"     -> topicId,
            "category"    -> category,
            "postId"      -> postId
          )
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          $doc("invitedBy" -> invitedBy, "studyName" -> studyName, "studyId" -> studyId)
        case p: PrivateMessage             => PrivateMessageHandler.writeTry(p).get
        case t: TeamJoined                 => TeamJoinedHandler.writeTry(t).get
        case x: TitledTournamentInvitation => TitledTournamentInvitationHandler.writeTry(x).get
        case x: GameEnd                    => GameEndHandler.writeTry(x).get
        case x: PlanStart                  => PlanStartHandler.writeTry(x).get
        case x: PlanExpire                 => PlanExpireHandler.writeTry(x).get
        case x: RatingRefund               => RatingRefundHandler.writeTry(x).get
        case ReportedBanned                => $empty
        case CoachReview                   => $empty
        case x: CorresAlarm                => CorresAlarmHandler.writeTry(x).get
        case x: IrwinDone                  => IrwinDoneHandler.writeTry(x).get
        case x: KaladinDone                => KaladinDoneHandler.writeTry(x).get
        case x: GenericLink                => GenericLinkHandler.writeTry(x).get
    } ++ $doc("type" -> notificationContent.key)

    private def readMentionedNotification(reader: Reader): MentionedInThread =
      val mentionedBy = reader.get[MentionedBy]("mentionedBy")
      val topic       = reader.get[Topic]("topic")
      val topicId     = reader.get[TopicId]("topicId")
      val category    = reader.get[Category]("category")
      val postNumber  = reader.get[PostId]("postId")

      MentionedInThread(mentionedBy, topic, topicId, category, postNumber)

    private def readInvitedStudyNotification(reader: Reader): NotificationContent =
      val invitedBy = reader.get[InvitedBy]("invitedBy")
      val studyName = reader.get[StudyName]("studyName")
      val studyId   = reader.get[StudyId]("studyId")

      InvitedToStudy(invitedBy, studyName, studyId)

    def reads(reader: Reader): NotificationContent =
      reader.str("type") match
        case "mention"        => readMentionedNotification(reader)
        case "invitedStudy"   => readInvitedStudyNotification(reader)
        case "privateMessage" => PrivateMessageHandler.readTry(reader.doc).get
        case "teamJoined"     => TeamJoinedHandler.readTry(reader.doc).get
        case "titledTourney"  => TitledTournamentInvitationHandler.readTry(reader.doc).get
        case "gameEnd"        => GameEndHandler.readTry(reader.doc).get
        case "planStart"      => PlanStartHandler.readTry(reader.doc).get
        case "planExpire"     => PlanExpireHandler.readTry(reader.doc).get
        case "ratingRefund"   => RatingRefundHandler.readTry(reader.doc).get
        case "reportedBanned" => ReportedBanned
        case "coachReview"    => CoachReview
        case "corresAlarm"    => CorresAlarmHandler.readTry(reader.doc).get
        case "irwinDone"      => IrwinDoneHandler.readTry(reader.doc).get
        case "kaladinDone"    => KaladinDoneHandler.readTry(reader.doc).get
        case "genericLink"    => GenericLinkHandler.readTry(reader.doc).get

    def writes(writer: Writer, n: NotificationContent): Bdoc = writeNotificationContent(n)

  private[notify] given BSONDocumentHandler[Notification] = Macros.handler
