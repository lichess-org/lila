package controllers

import lila.app._
import lila.common.paginator.PaginatorJson
import lila.notify.Notification.Notifies

import play.api.libs.json._
import views.html

object Notify extends LilaController {

  val env = Env.notifyModule

  import env.jsonHandlers._

  def recent(page: Int) = Auth { implicit ctx =>
    me =>
      val notifies = Notifies(me.id)
      env.api.getNotificationsAndCount(notifies, page) map { res =>
        Ok(Json toJson res) as JSON
      }
  }
}
