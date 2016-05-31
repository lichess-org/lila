package controllers

import lila.app._
import lila.notify.Notification.Notifies

import play.api.libs.json._
import views.html

object Notify extends LilaController {

  import lila.notify.JSONHandlers._

  val env = Env.notif

  val appMaxNotifications = 10

  def recent = Auth { implicit ctx =>
    me =>
      val notifies = Notifies(me.id)
      env.notifyApi.getNotifications(notifies, 1, appMaxNotifications) map {
        notifications => Ok(Json.toJson(notifications.currentPageResults)) as JSON
      }
  }

  def markAllAsRead = Auth {
    implicit ctx =>
      me =>
        val userId = Notifies(me.id)
        env.notifyApi.getNotifications(userId, 1, appMaxNotifications) flatMap { notifications =>
          notifications.currentPageResults.exists(_.unread).?? {
            env.notifyApi.markAllRead(userId)
          } inject {
            Ok(Json.toJson(notifications.currentPageResults)) as JSON
          }
        }
  }

  def notificationsPage = Auth { implicit ctx =>
    me =>
      val notifies = Notifies(me.id)
      env.notifyApi.getNotifications(notifies, 1, perPage = 100) map {
        notifications => Ok(html.notify.view(notifications.currentPageResults.toList))
      }
  }

}
