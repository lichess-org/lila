package views.html
package account

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object close {

  def apply(u: lidraughts.user.User, form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.closeAccount.txt()}",
    active = "close",
    evenMoreCss = cssTag("form3.css")
  ) {
      div(cls := "content_box small_box")(
        div(cls := "signup_box")(
          h1(dataIcon := "j", cls := "lidraughts_title text")(trans.closeAccount.frag()),
          st.form(cls := "form3", action := routes.Account.closeConfirm, method := "POST")(
            div(cls := "form-group")(trans.closeAccountExplanation.frag()),
            form3.passwordNoAutocomplete(form("passwd"), trans.password.frag()),
            form3.actions(
              a(href := routes.User.show(u.username))(trans.changedMindDoNotCloseAccount.frag()),
              form3.submit(trans.closeAccount.frag(), icon = "j".some)
            )
          )
        )
      )
    }
}
