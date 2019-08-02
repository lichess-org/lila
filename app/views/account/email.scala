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
          trans.changeEmail(),
          ctx.req.queryString.contains("ok") option
            frag(" ", i(cls := "is-green", dataIcon := "E"))
        ),
        postForm(cls := "form3", action := routes.Account.emailApply)(
          form3.password(form("passwd"), trans.password()),
          form3.group(form("email"), trans.email())(form3.input(_, typ = "email")(required)),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
}
