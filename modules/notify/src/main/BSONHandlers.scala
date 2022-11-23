package lila.notify

import chess.Color
import reactivemongo.api.bson.*

import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.{ *, given }
import lila.db.{ dsl, BSON }

private object BSONHandlers:
  private given PrivateMessageHandler: BSONDocumentHandler[PrivateMessage]       = Macros.handler
  private given MentionedInThreadHandler: BSONDocumentHandler[MentionedInThread] = Macros.handler
  private given InvitedToStudyHandler: BSONDocumentHandler[InvitedToStudy]       = Macros.handler
  private given TeamJoinedHandler: BSONDocumentHandler[TeamJoined]               = Macros.handler
  private given GameEndHandler: BSONDocumentHandler[GameEnd]                     = Macros.handler
  private given TitledTournamentInvitationHandler: BSONDocumentHandler[TitledTournamentInvitation] =
    Macros.handler
  private given PlanStartHandler: BSONDocumentHandler[PlanStart]       = Macros.handler
  private given PlanExpireHandler: BSONDocumentHandler[PlanExpire]     = Macros.handler
  private given RatingRefundHandler: BSONDocumentHandler[RatingRefund] = Macros.handler
  private given CorresAlarmHandler: BSONDocumentHandler[CorresAlarm]   = Macros.handler
  private given IrwinDoneHandler: BSONDocumentHandler[IrwinDone]       = Macros.handler
  private given KaladinDoneHandler: BSONDocumentHandler[KaladinDone]   = Macros.handler
  private given GenericLinkHandler: BSONDocumentHandler[GenericLink]   = Macros.handler
  private given StreamStartHandler: BSONDocumentHandler[StreamStart]   = Macros.handler

  given lila.db.BSON[NotificationContent] with
    private def writeNotificationContent(notificationContent: NotificationContent) = {
      notificationContent match
        case x: MentionedInThread          => MentionedInThreadHandler.writeTry(x).get
        case x: InvitedToStudy             => InvitedToStudyHandler.writeTry(x).get
        case x: PrivateMessage             => PrivateMessageHandler.writeTry(x).get
        case x: TeamJoined                 => TeamJoinedHandler.writeTry(x).get
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
        case x: StreamStart                => StreamStartHandler.writeTry(x).get
    } ++ $doc("type" -> notificationContent.key)

    def reads(reader: Reader): NotificationContent =
      reader.str("type") match
        case "mention"        => MentionedInThreadHandler.readTry(reader.doc).get
        case "invitedStudy"   => InvitedToStudyHandler.readTry(reader.doc).get
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
        case "streamStart"    => StreamStartHandler.readTry(reader.doc).get

    def writes(writer: Writer, n: NotificationContent): Bdoc = writeNotificationContent(n)

  given BSONDocumentHandler[Notification] = Macros.handler
