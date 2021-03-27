package controllers

import play.api.libs.json._

import lila.app._
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => t }
import lila.notify.Notification.Notifies

final class Notify(env: Env) extends LilaController(env) {

  import lila.common.paginator.PaginatorJson._

  private val i18nKeys: List[lila.i18n.MessageKey] = List(
    t.mentionedYouInX,
    t.xMentionedYouInY,
    t.invitedYouToX,
    t.xInvitedYouToY,
    t.youAreNowPartOfTeam,
    t.youHaveJoinedTeamX,
    t.thankYou,
    t.someoneYouReportedWasBanned,
    t.victory,
    t.defeat,
    t.draw,
    t.congratsYouWon,
    t.gameVsX,
    t.resVsX,
    t.youJustBecamePatron,
    t.patronAccountExpired,
    t.pleaseReconsiderRenewIt,
    t.newPendingReview,
    t.someoneReviewedYourCoachProfile,
    t.lostAgainstTOSViolator,
    t.refundXpointsTimeControlY,
    t.timeAlmostUp
  ).map(_.key)

  def recent(page: Int) =
    Auth { implicit ctx => me =>
      XhrOrRedirectHome {
        val notifies = Notifies(me.id)
        env.notifyM.api.getNotificationsAndCount(notifies, page) map { x =>
          JsonOk(
            env.notifyM.jsonHandlers.andUnreadWrites.writes(x) ++ Json.obj(
              "i18n" -> i18nJsObject(i18nKeys)
            )
          )
        }
      }
    }
}
