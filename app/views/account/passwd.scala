package views.html
package account

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object passwd:

  def apply(form: play.api.data.Form[?])(implicit ctx: Context) =
    account.layout(
      title = trans.changePassword.txt(),
      active = "password",
      evenMoreJs = frag(
        embedJsUnsafeLoadThen("""
          lichess.loadModule('passwordComplexity').then(() =>
            passwordComplexity.addPasswordChangeListener('form3-newPasswd1')
          )""")
      )
    ) {
      div(cls := "account box box-pad")(
        h1(cls := "box__top")(trans.changePassword()),
        standardFlash(),
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
