package controllers

import play.api.libs.json._

import lila.app._
import lila.notify.Notification.Notifies

final class Notify(env: Env) extends LilaController(env) {

  def recent(page: Int) =
    Auth { implicit ctx => me =>
      XhrOrRedirectHome {
        env.notifyM.api
          .getNotificationsAndCount(Notifies(me.id), page) map env.notifyM.jsonHandlers.apply map JsonOk
      }
    }
}
