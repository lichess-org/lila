package lila.app
package templating

import lila.user.User
import lila.notification.Env.{ current => notificationEnv }

import play.twirl.api.Html
import play.api.mvc.Call

trait NotificationHelper {

  def renderNotifications(user: User): Html = {
    val notifs = notificationEnv.api get user.id take 2 map { notif =>
      views.html.notification.view(notif.id, notif.from)(Html(notif.html))
    }
    Html(notifs.foldLeft("")(_ + _.body))
  }
}
