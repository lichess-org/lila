package lila.notification

import lila.user.{ User, UserHelper }

import play.api.templates.Html
import play.api.mvc.Call

trait NotificationHelper { 

  protected def env: CoreEnv
  private def api = env.notificationApi

  def notifications(user: User): Html = {
    val notifs = api get user.id take 2 map { notif =>
      views.html.notification.view(notif.id, notif.from)(Html(notif.html)) 
    } 
    notifs.foldLeft(Html(""))(_ += _)
  }
}
