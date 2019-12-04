package controllers

import lila.app._
import lila.notify.Notification.Notifies

import play.api.libs.json._

final class Notify(env: Env) extends LilaController(env) {

  val env = env.notifyModule

  import env.jsonHandlers._

  def recent(page: Int) = Auth { implicit ctx => me =>
    XhrOrRedirectHome {
      val notifies = Notifies(me.id)
      env.api.getNotificationsAndCount(notifies, page) map { res =>
        Ok(Json toJson res) as JSON
      }
    }
  }
}
