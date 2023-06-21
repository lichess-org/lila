package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object username:

  def apply(u: lila.user.User, form: play.api.data.Form[?])(using PageContext) =
    account.layout(
      title = s"${u.username} - ${trans.editProfile.txt()}",
      active = "username"
    ) {
      div(cls := "account box box-pad")(
        h1(cls := "box__top")(trans.changeUsername()),
        standardFlash,
        postForm(cls := "form3", action := routes.Account.usernameApply)(
          form3.globalError(form),
          form3.group(form("username"), trans.username(), help = trans.changeUsernameDescription().some)(
            form3.input(_)(autofocus, required, autocomplete := "username")
          ),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
