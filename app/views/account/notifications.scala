package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object notifications {

  def apply(u: lila.user.User)(implicit ctx: Context) = account.layout(
    title = s"${u.username} - Notifications",
    active = "notifications",
    evenMoreJs = jsTag("notifications.js")
  ) {
      div(cls := "account box box-pad")(
        h1("Notifications"),
        "VAPID:", st.input(tpe := "text", id := "vapid", value := lila.app.Env.push.VapidPublicKey),
        br,
        "Click this:", st.input(tpe := "checkbox", id := "subscribed", disabled), " Subscribed"
      )
    }
}
