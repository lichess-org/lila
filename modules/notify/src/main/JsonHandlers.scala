package lila.notify

import lila.common.LightUser
import play.api.libs.json._
import lila.i18n.{ I18nKeys => trans }

import lila.common.Json.jodaWrites
import play.api.i18n.Lang
import lila.i18n.JsDump

final class JSONHandlers(getLightUser: LightUser.GetterSync) {

  implicit val notificationWrites: Writes[Notification] = new Writes[Notification] {

    private def writeBody(notificationContent: NotificationContent) = {
      notificationContent match {
        case MentionedInThread(mentionedBy, topic, _, category, postId) =>
          Json.obj(
            "mentionedBy" -> getLightUser(mentionedBy),
            "topic"       -> topic,
            "category"    -> category,
            "postId"      -> postId
          )
        case StreamStart(streamerId, streamerName) =>
          Json.obj(
            "sid"  -> streamerId,
            "name" -> streamerName
          )
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          Json.obj(
            "invitedBy" -> getLightUser(invitedBy),
            "studyName" -> studyName,
            "studyId"   -> studyId
          )
        case PrivateMessage(senderId, text) =>
          Json.obj(
            "user" -> getLightUser(senderId),
            "text" -> text
          )
        case TeamJoined(id, name) =>
          Json.obj(
            "id"   -> id,
            "name" -> name
          )
        case ReportedBanned | CoachReview => Json.obj()
        case TitledTournamentInvitation(id, text) =>
          Json.obj(
            "id"   -> id,
            "text" -> text
          )
        case GameEnd(gameId, opponentId, win) =>
          Json.obj(
            "id"       -> gameId,
            "opponent" -> opponentId.flatMap(getLightUser),
            "win"      -> win
          )
        case _: PlanStart  => Json.obj()
        case _: PlanExpire => Json.obj()
        case RatingRefund(perf, points) =>
          Json.obj(
            "perf"   -> perf,
            "points" -> points
          )
        case CorresAlarm(gameId, opponent) =>
          Json.obj(
            "id" -> gameId,
            "op" -> opponent
          )
        case IrwinDone(userId) =>
          Json.obj(
            "user" -> getLightUser(userId)
          )
        case KaladinDone(userId) =>
          Json.obj(
            "user" -> getLightUser(userId)
          )
        case GenericLink(url, title, text, icon) =>
          Json.obj(
            "url"   -> url,
            "title" -> title,
            "text"  -> text,
            "icon"  -> icon
          )
      }
    }

    def writes(notification: Notification) =
      Json.obj(
        "content" -> writeBody(notification.content),
        "type"    -> notification.content.key,
        "read"    -> notification.read,
        "date"    -> notification.createdAt
      )
  }

  import lila.common.paginator.PaginatorJson._
  implicit val andUnreadWrites: OWrites[Notification.AndUnread] = Json.writes[Notification.AndUnread]

  private val i18nKeys: List[lila.i18n.MessageKey] = List(
    trans.mentionedYouInX,
    trans.xMentionedYouInY,
    trans.startedStreaming,
    trans.xStartedStreaming,
    trans.invitedYouToX,
    trans.xInvitedYouToY,
    trans.youAreNowPartOfTeam,
    trans.youHaveJoinedTeamX,
    trans.thankYou,
    trans.someoneYouReportedWasBanned,
    trans.victory,
    trans.defeat,
    trans.draw,
    trans.congratsYouWon,
    trans.gameVsX,
    trans.resVsX,
    trans.newPendingReview,
    trans.someoneReviewedYourCoachProfile,
    trans.lostAgainstTOSViolator,
    trans.refundXpointsTimeControlY,
    trans.timeAlmostUp
  ).map(_.key)

  def apply(notify: Notification.AndUnread)(implicit lang: Lang): JsObject =
    andUnreadWrites.writes(notify) ++ Json.obj(
      "i18n" -> JsDump.keysToObject(i18nKeys, lang)
    )
}
