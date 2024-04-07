package views.html
package account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object passwd:

  def apply(form: play.api.data.Form[?])(using PageContext) =
    account.layout(
      title = trans.site.changePassword.txt(),
      active = "password",
      modules = jsModuleInit("bits.passwordComplexity")
    ):
      div(cls := "box box-pad")(
        h1(cls := "box__top")(trans.site.changePassword()),
        standardFlash | flashMessage("warning")(trans.site.passwordSuggestion()),
        postForm(cls := "form3", action := routes.Account.passwdApply)(
          form3.passwordModified(form("oldPasswd"), trans.site.currentPassword())(
            autofocus,
            autocomplete := "current-password"
          ),
          form3.passwordModified(form("newPasswd1"), trans.site.newPassword())(
            autocomplete := "new-password"
          ),
          form3.passwordComplexityMeter(trans.site.newPasswordStrength()),
          form3.passwordModified(form("newPasswd2"), trans.site.newPasswordAgain())(
            autocomplete := "new-password"
          ),
          form3.globalError(form),
          form3.action(form3.submit(trans.site.apply()))
        )
      )
