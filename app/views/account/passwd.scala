package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object passwd {

  def apply(form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = trans.changePassword.txt(),
    active = "password"
  ) {
      div(cls := "account box box-pad")(
        h1(
          trans.changePassword(),
          ctx.req.queryString.contains("ok") option
            frag(" ", i(cls := "is-green", dataIcon := "E"))
        ),
        st.form(cls := "form3", action := routes.Account.passwdApply, method := "POST")(
          form3.password(form("oldPasswd"), trans.currentPassword()),
          form3.password(form("newPasswd1"), trans.newPassword()),
          form3.password(form("newPasswd2"), trans.newPasswordAgain()),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
}
