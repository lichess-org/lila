package controllers

import lila.app._
import lila.notify.Notification.Notifies

import play.api.libs.json._
import views.html

object Notif extends LilaController {

  import lila.notify.JSONHandlers._

  val env = Env.notif

  def recent = Auth { implicit ctx =>
    me =>
      val notifies = Notifies(me.id)
      env.notifyApi.getNotifications(notifies, 1, 10) map {
        notifications => Ok(Json.toJson(notifications.currentPageResults)) as JSON
      }
  }

  def markAllAsRead = Auth {
    implicit ctx =>
      me =>
        val userId = Notifies(me.id)
        env.notifyApi.markAllRead(userId)
  }

  def notificationsPage = Auth { implicit ctx =>
    me =>
      val notifies = Notifies(me.id)
      env.notifyApi.getNotifications(notifies, 1, perPage = 100) map {
        notifications => Ok(html.notifications.view(notifications.currentPageResults.toList))
      }
  }

}
