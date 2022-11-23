package lila.notify

import lila.common.LightUser
import play.api.libs.json.*
import lila.i18n.I18nKeys as trans

import lila.common.Json.given
import play.api.i18n.Lang
import lila.i18n.JsDump

final class JSONHandlers(getLightUser: LightUser.GetterSync):

  given Writes[Notification] with

    private def writeBody(notificationContent: NotificationContent) =
      notificationContent match
        case MentionedInThread(mentionedBy, topic, _, category, postId) =>
          Json.obj(
            "mentionedBy" -> getLightUser(mentionedBy.value),
            "topic"       -> topic.value,
            "category"    -> category.value,
            "postId"      -> postId.value
          )
        case InvitedToStudy(invitedBy, studyName, studyId) =>
          Json.obj(
            "invitedBy" -> getLightUser(invitedBy.value),
            "studyName" -> studyName,
            "studyId"   -> studyId
          )
        case PrivateMessage(senderId, text) =>
          Json.obj(
            "user" -> getLightUser(senderId.value),
            "text" -> text.value
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
            "opponent" -> opponentId.map(_.value).flatMap(getLightUser),
            "win"      -> win.map(_.value)
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

    def writes(notification: Notification) =
      Json.obj(
        "content" -> writeBody(notification.content),
        "type"    -> notification.content.key,
        "read"    -> notification.read.value,
        "date"    -> notification.createdAt
      )

  import lila.common.paginator.PaginatorJson.given
  given Writes[Notification.UnreadCount] = Writes { v =>
    JsNumber(v.value)
  }
  given OWrites[Notification.AndUnread] = Json.writes

  given OWrites[NewNotification] = OWrites { newNotification =>
    Json.obj(
      "notification" -> newNotification.notification,
      "unread"       -> newNotification.unreadNotifications
    )
  }

  private val i18nKeys: List[lila.i18n.MessageKey] = List(
    trans.mentionedYouInX,
    trans.xMentionedYouInY,
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

  def apply(notify: Notification.AndUnread)(using lang: Lang) =
    Json.toJsObject(notify) ++ Json.obj(
      "i18n" -> JsDump.keysToObject(i18nKeys, lang)
    )
