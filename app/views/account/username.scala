package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object username {

  def apply(u: lila.user.User, form: play.api.data.Form[_])(implicit ctx: Context) =
    account.layout(
      title = s"${u.username} - ${trans.editProfile.txt()}",
      active = "username"
    ) {
      div(cls := "account box box-pad")(
        h1(trans.changeUsername()),
        standardFlash(),
        postForm(cls := "form3", action := routes.Account.usernameApply)(
          form3.globalError(form),
          form3.group(form("username"), trans.username(), help = trans.changeUsernameDescription().some)(
            form3.input(_)(autofocus, required, autocomplete := "username")
          ),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
}
