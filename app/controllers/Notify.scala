package controllers

import lila.app._
import lila.notify.Notification.Notifies

import play.api.libs.json._

final class Notify(env: Env) extends LilaController(env) {

  import env.notifyM.jsonHandlers._

  def recent(page: Int) =
    Auth { implicit ctx => me =>
      XhrOrRedirectHome {
        val notifies = Notifies(me.id)
        env.notifyM.api.getNotificationsAndCount(notifies, page) map { res =>
          Ok(Json toJson res) as JSON
        }
      }
    }

  def clear =
    Auth { implicit ctx => me =>
      XhrOrRedirectHome { (env.notifyM.api.remove(Notifies(me.id))) }
    }
}
