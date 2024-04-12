package lila.notify

import play.api.libs.json.*
import scalalib.Json.given

import lila.common.Json.given
import lila.core.LightUser
import lila.core.i18n.I18nKey.{ site as trans }
import lila.core.i18n.{ I18nKey, Translate, JsDump }
import lila.core.notify.*

final class JSONHandlers(getLightUser: LightUser.GetterSync, jsDump: JsDump):

  given Writes[Notification] with

    private def writeBody(content: NotificationContent) = content match
      case MentionedInThread(mentionedBy, topic, _, category, postId) =>
        Json.obj(
          "mentionedBy" -> getLightUser(mentionedBy),
          "topic"       -> topic,
          "category"    -> category,
          "postId"      -> postId
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
          "id"       -> gameId.value,
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
      case StreamStart(streamerId, streamerName) =>
        Json.obj(
          "sid"  -> streamerId,
          "name" -> streamerName
        )
      case BroadcastRound(url, title, text) =>
        Json.obj(
          "url"   -> url,
          "title" -> title,
          "text"  -> text
        )
    def writes(notification: Notification) =
      Json.obj(
        "content" -> writeBody(notification.content),
        "type"    -> notification.content.key,
        "read"    -> notification.read.value,
        "date"    -> notification.createdAt
      )

  given OWrites[Notification.AndUnread] = Json.writes

  private val i18nKeys: List[I18nKey] = List(
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
    trans.lostAgainstTOSViolator,
    trans.refundXpointsTimeControlY,
    trans.timeAlmostUp
  )

  def apply(notify: Notification.AndUnread)(using Translate) =
    Json.toJsObject(notify) ++ Json.obj(
      "i18n" -> jsDump.keysToObject(i18nKeys)
    )
