package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object passwd:

  def apply(form: play.api.data.Form[?])(using PageContext) =
    account.layout(
      title = trans.changePassword.txt(),
      active = "password",
      evenMoreJs = jsModuleInit("passwordComplexity", "'form3-newPasswd1'")
    ) {
      div(cls := "account box box-pad")(
        h1(cls := "box__top")(trans.changePassword()),
        standardFlash | flashMessage("warning")(trans.passwordSuggestion()),
        postForm(cls := "form3", action := routes.Account.passwdApply)(
          form3.passwordModified(form("oldPasswd"), trans.currentPassword())(
            autofocus,
            autocomplete := "current-password"
          ),
          form3.passwordModified(form("newPasswd1"), trans.newPassword())(autocomplete := "new-password"),
          form3.passwordComplexityMeter(trans.newPasswordStrength()),
          form3.passwordModified(form("newPasswd2"), trans.newPasswordAgain())(
            autocomplete := "new-password"
          ),
          form3.globalError(form),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
