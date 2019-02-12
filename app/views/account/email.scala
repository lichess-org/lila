package views.html
package account

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object email {

  def apply(u: lidraughts.user.User, form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = trans.changeEmail.txt(),
    active = "email"
  ) {
      div(cls := "account box box-pad")(
        h1(
          trans.changeEmail.frag(),
          ctx.req.queryString.contains("ok") option
            frag(" ", i(cls := "is-green", dataIcon := "E"))
        ),
        st.form(cls := "form3", action := routes.Account.emailApply, method := "POST")(
          form3.password(form("passwd"), trans.password.frag()),
          form3.group(form("email"), trans.email.frag())(form3.input(_, typ = "email")),
          form3.action(form3.submit(trans.apply.frag()))
        )
      )
    }
}
