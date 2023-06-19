package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object email:

  def apply(form: play.api.data.Form[?])(using PageContext) =
    account.layout(
      title = trans.changeEmail.txt(),
      active = "email"
    ) {
      div(cls := "account box box-pad")(
        h1(cls := "box__top")(trans.changeEmail()),
        standardFlash | flashMessage("warning")(trans.emailSuggestion()),
        postForm(cls := "form3", action := routes.Account.emailApply)(
          form3.passwordModified(form("passwd"), trans.password())(autofocus),
          form3.group(form("email"), trans.email())(form3.input(_, typ = "email")(required)),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
