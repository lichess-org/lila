package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object passwd {

  def apply(form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = trans.changePassword.txt(),
    active = "password",
    evenMoreCss = cssTag("form3.css")
  ) {
      div(cls := "content_box small_box")(
        div(cls := "signup_box")(
          h1(cls := "lichess_title")(
            trans.changePassword.frag(),
            raw(ctx.req.queryString.contains("ok") ?? """ <span class="is-green" data-icon="E"></span>""")
          ),
          st.form(cls := "form3", action := routes.Account.passwdApply, method := "POST")(
            form3.password(form("oldPasswd"), trans.currentPassword.frag()),
            form3.password(form("newPasswd1"), trans.newPassword.frag()),
            form3.password(form("newPasswd2"), trans.newPasswordAgain.frag()),
            form3.actionHtml(form3.submit(trans.apply.frag()))
          )
        )
      )
    }
}
