package views.html
package account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object email:

  def apply(form: play.api.data.Form[?])(using PageContext) =
    account.layout(
      title = trans.site.changeEmail.txt(),
      active = "email"
    ):
      div(cls := "box box-pad")(
        h1(cls := "box__top")(trans.site.changeEmail()),
        standardFlash | flashMessage("warning")(trans.site.emailSuggestion()),
        postForm(cls := "form3", action := routes.Account.emailApply)(
          form3.passwordModified(form("passwd"), trans.site.password())(autofocus),
          form3.group(form("email"), trans.site.email())(form3.input(_, typ = "email")(required)),
          form3.action(form3.submit(trans.site.apply()))
        )
      )
