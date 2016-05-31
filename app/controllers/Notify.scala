package controllers

import lila.app._
import lila.common.paginator.PaginatorJson
import lila.notify.Notification.Notifies

import play.api.libs.json._
import views.html

object Notify extends LilaController {

  val env = Env.notifyModule

  import env.jsonHandlers._

  val appMaxNotifications = 10

  def recent(page: Int) = Auth { implicit ctx =>
    me =>
      val notifies = Notifies(me.id)
      env.notifyApi.getNotifications(notifies, page, appMaxNotifications) map {
        notifications => Ok(PaginatorJson(notifications)) as JSON
      }
  }

  def markAllAsRead = Auth {
    implicit ctx =>
      me =>
        val userId = Notifies(me.id)
        env.notifyApi.getNotifications(userId, 1, appMaxNotifications) flatMap { notifications =>
          env.notifyApi.markAllRead(userId) inject {
            Ok(PaginatorJson(notifications)) as JSON
          }
        }
  }
}
