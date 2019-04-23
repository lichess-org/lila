package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object email {

  def apply(u: lila.user.User, form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = trans.changeEmail.txt(),
    active = "email"
  ) {
      div(cls := "account box box-pad")(
        h1(
          trans.changeEmail(),
          ctx.req.queryString.contains("ok") option
            frag(" ", i(cls := "is-green", dataIcon := "E"))
        ),
        st.form(cls := "form3", action := routes.Account.emailApply, method := "POST")(
          form3.password(form("passwd"), trans.password()),
          form3.group(form("email"), trans.email())(form3.input(_, typ = "email")(required)),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
}
