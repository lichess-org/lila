package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object view {

  def apply(
      u: User,
      info: lila.app.mashup.UserInfo,
      social: lila.app.mashup.UserInfo.Social,
      emails: User.Emails,
      erased: User.Erased
  )(implicit
      ctx: Context
  ): Frag =
    views.html.base.layout(
      title = s"${u.username} mod",
      moreJs = jsAt("compiled/user-mod2.js"),
      moreCss = cssTag("mod.user2"),
      wrapClass = "full-screen-force"
    ) {
      main(
        div(cls := "box")(
          h1(u.username)
        ),
        views.html.user.show.header(u, info, lila.app.mashup.UserInfo.Angle.Activity, social)
      )
    }
}
