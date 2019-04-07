package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object username {

  def apply(u: lila.user.User, form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.editProfile.txt()}",
    active = "editUsername",
    evenMoreCss = cssTag("form3.css")
  ) {
      div(cls := "content_box small_box")(
        h1(cls := "lichess_title text", dataIcon := "*")(trans.editProfile()),
        st.form(cls := "form3", action := routes.Account.usernameApply, method := "POST")(
          form3.globalError(form),
          form3.group(form("userName"), trans.username.frag(), half = true, help = trans.usernameDescription.frag().some)(form3.input(_)),
          form3.actionHtml(form3.submit(trans.apply.frag()))
        )
      )
    }
}
