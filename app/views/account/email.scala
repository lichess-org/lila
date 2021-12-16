package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object email {

  def apply(form: play.api.data.Form[_])(implicit ctx: Context) =
    account.layout(
      title = trans.changeEmail.txt(),
      active = "email"
    ) {
      div(cls := "account box box-pad")(
        h1(trans.changeEmail()),
        standardFlash(),
        postForm(cls := "form3", action := routes.Account.emailApply)(
          form3.passwordModified(form("passwd"), trans.password())(autofocus),
          form3.group(form("email"), trans.email())(form3.input(_, typ = "email")(required)),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
}
