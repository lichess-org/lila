package lila.notify

import chess.Color
import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.{ *, given }
import lila.notify.MentionedInThread.*
import lila.notify.Notification.*
import reactivemongo.api.bson.*

private object BSONHandlers:

  private given BSONDocumentHandler[PrivateMessage]             = Macros.handler
  private given BSONDocumentHandler[TeamJoined]                 = Macros.handler
  private given BSONDocumentHandler[GameEnd]                    = Macros.handler
  private given BSONDocumentHandler[TitledTournamentInvitation] = Macros.handler
  private given BSONDocumentHandler[PlanStart]                  = Macros.handler
  private given BSONDocumentHandler[PlanExpire]                 = Macros.handler
  private given BSONDocumentHandler[RatingRefund]               = Macros.handler
  private given BSONDocumentHandler[CorresAlarm]                = Macros.handler
  private given BSONDocumentHandler[IrwinDone]                  = Macros.handler
  private given BSONDocumentHandler[KaladinDone]                = Macros.handler
  private given BSONDocumentHandler[GenericLink]                = Macros.handler
  private given BSONReader[MentionedInThread]                   = Macros.reader
  private given BSONReader[InvitedToStudy]                      = Macros.reader

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
        case p: PrivateMessage             => summon[BSONHandler[PrivateMessage]].writeTry(p).get
        case t: TeamJoined                 => summon[BSONHandler[TeamJoined]].writeTry(t).get
        case x: TitledTournamentInvitation => summon[BSONHandler[TitledTournamentInvitation]].writeTry(x).get
        case x: GameEnd                    => summon[BSONHandler[GameEnd]].writeTry(x).get
        case x: PlanStart                  => summon[BSONHandler[PlanStart]].writeTry(x).get
        case x: PlanExpire                 => summon[BSONHandler[PlanExpire]].writeTry(x).get
        case x: RatingRefund               => summon[BSONHandler[RatingRefund]].writeTry(x).get
        case ReportedBanned                => $empty
        case CoachReview                   => $empty
        case x: CorresAlarm                => summon[BSONHandler[CorresAlarm]].writeTry(x).get
        case x: IrwinDone                  => summon[BSONHandler[IrwinDone]].writeTry(x).get
        case x: KaladinDone                => summon[BSONHandler[KaladinDone]].writeTry(x).get
        case x: GenericLink                => summon[BSONHandler[GenericLink]].writeTry(x).get
    } ++ $doc("type" -> notificationContent.key)

    def reads(reader: Reader): NotificationContent =
      reader.str("type") match
        case "mention"        => reader.as[MentionedInThread]
        case "invitedStudy"   => reader.as[InvitedToStudy]
        case "privateMessage" => reader.as[PrivateMessage]
        case "teamJoined"     => reader.as[TeamJoined]
        case "titledTourney"  => reader.as[TitledTournamentInvitation]
        case "gameEnd"        => reader.as[GameEnd]
        case "planStart"      => reader.as[PlanStart]
        case "planExpire"     => reader.as[PlanExpire]
        case "ratingRefund"   => reader.as[RatingRefund]
        case "reportedBanned" => ReportedBanned
        case "coachReview"    => CoachReview
        case "corresAlarm"    => reader.as[CorresAlarm]
        case "irwinDone"      => reader.as[IrwinDone]
        case "kaladinDone"    => reader.as[KaladinDone]
        case "genericLink"    => reader.as[GenericLink]

    def writes(writer: Writer, n: NotificationContent): Bdoc = writeNotificationContent(n)

  private[notify] given BSONDocumentHandler[Notification] = Macros.handler
