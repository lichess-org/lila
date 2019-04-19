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
        st.input(tpe := "checkbox", id := "subscribed", disabled), " Subscribed"
      )
    }
}
