package lila.notify

import reactivemongo.api.bson.*

import lila.db.BSON.{ Reader, Writer }
import lila.db.dsl.{ *, given }
import lila.core.notify.*

private object BSONHandlers:

  import NotificationContent.*
  private given BSONDocumentHandler[StreamStart] = Macros.handler
  private given BSONDocumentHandler[PrivateMessage] = Macros.handler
  private given BSONDocumentHandler[TeamJoined] = Macros.handler
  private given BSONDocumentHandler[GameEnd] = Macros.handler
  private given BSONDocumentHandler[TitledTournamentInvitation] = Macros.handler
  private given BSONDocumentHandler[PlanStart] = Macros.handler
  private given BSONDocumentHandler[PlanExpire] = Macros.handler
  private given BSONDocumentHandler[RatingRefund] = Macros.handler
  private given BSONDocumentHandler[CorresAlarm] = Macros.handler
  private given BSONDocumentHandler[IrwinDone] = Macros.handler
  private given BSONDocumentHandler[KaladinDone] = Macros.handler
  private given BSONDocumentHandler[GenericLink] = Macros.handler
  private given BSONDocumentHandler[BroadcastRound] = Macros.handler
  private given BSONDocumentHandler[Recap] = Macros.handler
  private given mentionHandler: BSONDocumentHandler[MentionedInThread] = Macros.handler
  private given inviteHandler: BSONDocumentHandler[InvitedToStudy] = Macros.handler

  given lila.db.BSON[NotificationContent] with
    private def writeNotificationContent(notificationContent: NotificationContent) = {
      notificationContent match
        case x: MentionedInThread => mentionHandler.writeTry(x).get
        case x: InvitedToStudy => inviteHandler.writeTry(x).get
        case x: PrivateMessage => summon[BSONHandler[PrivateMessage]].writeTry(x).get
        case x: StreamStart => summon[BSONHandler[StreamStart]].writeTry(x).get
        case x: TeamJoined => summon[BSONHandler[TeamJoined]].writeTry(x).get
        case x: TitledTournamentInvitation => summon[BSONHandler[TitledTournamentInvitation]].writeTry(x).get
        case x: GameEnd => summon[BSONHandler[GameEnd]].writeTry(x).get
        case x: PlanStart => summon[BSONHandler[PlanStart]].writeTry(x).get
        case x: PlanExpire => summon[BSONHandler[PlanExpire]].writeTry(x).get
        case x: RatingRefund => summon[BSONHandler[RatingRefund]].writeTry(x).get
        case ReportedBanned => $empty
        case CoachReview => $empty
        case x: CorresAlarm => summon[BSONHandler[CorresAlarm]].writeTry(x).get
        case x: IrwinDone => summon[BSONHandler[IrwinDone]].writeTry(x).get
        case x: KaladinDone => summon[BSONHandler[KaladinDone]].writeTry(x).get
        case x: GenericLink => summon[BSONHandler[GenericLink]].writeTry(x).get
        case x: BroadcastRound => summon[BSONHandler[BroadcastRound]].writeTry(x).get
        case x: Recap => summon[BSONHandler[Recap]].writeTry(x).get
    } ++ $doc("type" -> notificationContent.key)

    def reads(reader: Reader): NotificationContent =
      reader.str("type") match
        case "mention" => reader.as[MentionedInThread]
        case "invitedStudy" => reader.as[InvitedToStudy]
        case "privateMessage" => reader.as[PrivateMessage]
        case "teamJoined" => reader.as[TeamJoined]
        case "titledTourney" => reader.as[TitledTournamentInvitation]
        case "gameEnd" => reader.as[GameEnd]
        case "planStart" => reader.as[PlanStart]
        case "planExpire" => reader.as[PlanExpire]
        case "ratingRefund" => reader.as[RatingRefund]
        case "reportedBanned" => ReportedBanned
        case "coachReview" => CoachReview
        case "corresAlarm" => reader.as[CorresAlarm]
        case "irwinDone" => reader.as[IrwinDone]
        case "kaladinDone" => reader.as[KaladinDone]
        case "genericLink" => reader.as[GenericLink]
        case "streamStart" => reader.as[StreamStart]
        case "broadcastRound" => reader.as[BroadcastRound]
        case "recap" => reader.as[Recap]

    def writes(w: Writer, n: NotificationContent): Bdoc = writeNotificationContent(n)

  private[notify] given BSONDocumentHandler[Notification] = Macros.handler
