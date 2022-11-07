package lila.notify

import chess.Color
import reactivemongo.api.bson._

import lila.db.{ dsl, BSON }
import lila.db.dsl._
import lila.db.BSON.{ Reader, Writer }

private object BSONHandlers {
  implicit val PrivateMessageHandler             = Macros.handler[PrivateMessage]
  implicit val MentionHandler                    = Macros.handler[MentionedInThread]
  implicit val InvitedToStudyHandler             = Macros.handler[InvitedToStudy]
  implicit val TeamJoinedHandler                 = Macros.handler[TeamJoined]
  implicit val GameEndHandler                    = Macros.handler[GameEnd]
  implicit val TitledTournamentInvitationHandler = Macros.handler[TitledTournamentInvitation]
  implicit val PlanStartHandler                  = Macros.handler[PlanStart]
  implicit val PlanExpireHandler                 = Macros.handler[PlanExpire]
  implicit val RatingRefundHandler               = Macros.handler[RatingRefund]
  implicit val CorresAlarmHandler                = Macros.handler[CorresAlarm]
  implicit val IrwinDoneHandler                  = Macros.handler[IrwinDone]
  implicit val KaladinDoneHandler                = Macros.handler[KaladinDone]
  implicit val GenericLinkHandler                = Macros.handler[GenericLink]
  implicit val StreamStartHandler                = Macros.handler[StreamStart]

  implicit val ColorBSONHandler = BSONBooleanHandler.as[Color](Color.fromWhite, _.white)
  implicit val NotificationContentHandler = new BSON[NotificationContent] {

    private def writeNotificationContent(notificationContent: NotificationContent) = {
      notificationContent match {
        case x: MentionedInThread          => MentionHandler.writeTry(x).get
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
      }
    } ++ $doc("type" -> notificationContent.key)

    def reads(reader: Reader): NotificationContent =
      reader.str("type") match {
        case "mention"        => MentionHandler.readTry(reader.doc).get
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
      }

    def writes(writer: Writer, n: NotificationContent): dsl.Bdoc = writeNotificationContent(n)
  }

  implicit val NotificationBSONHandler = Macros.handler[Notification]
}
