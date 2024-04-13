package views.html
package account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }

object username:

  def apply(u: User, form: play.api.data.Form[?])(using PageContext) =
    account.layout(
      title = s"${u.username} - ${trans.site.editProfile.txt()}",
      active = "username"
    ):
      div(cls := "box box-pad")(
        h1(cls := "box__top")(trans.site.changeUsername()),
        standardFlash,
        postForm(cls := "form3", action := routes.Account.usernameApply)(
          form3.globalError(form),
          form3.group(
            form("username"),
            trans.site.username(),
            help = trans.site.changeUsernameDescription().some
          )(
            form3.input(_)(autofocus, required, autocomplete := "username")
          ),
          form3.action(form3.submit(trans.site.apply()))
        )
      )
