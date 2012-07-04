package lila
package notification

import core.CoreEnv
import user.{ User, UserHelper }

import play.api.templates.Html
import play.api.mvc.Call

trait NotificationHelper { 

  protected def env: CoreEnv
  private def api = env.notificationApi

  def notifications(user: User): Html = {
    val notifs = api get user take 2 map { views.html.notification.view(_) } 
    notifs.foldLeft(Html(""))(_ + _)
  }

}
