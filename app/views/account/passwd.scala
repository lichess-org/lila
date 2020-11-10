package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object passwd {

  def apply(form: play.api.data.Form[_])(implicit ctx: Context) =
    account.layout(
      title = trans.changePassword.txt(),
      active = "password",
      evenMoreJs = frag(
        jsModule("passwordComplexity"),
        embedJsUnsafeLoadThen("""passwordComplexity.addPasswordChangeListener('form3-newPasswd1')"""),
        jsAt("javascripts/vendor/zxcvbn.min.js"),
      )
    ) {
      div(cls := "account box box-pad")(
        h1(trans.changePassword()),
        standardFlash(),
        postForm(cls := "form3", action := routes.Account.passwdApply())(
          form3.passwordModified(form("oldPasswd"), trans.currentPassword())(
            autofocus,
            autocomplete := "current-password"
          ),
          form3.passwordModified(form("newPasswd1"), trans.newPassword())(autocomplete := "new-password"),
          form3.passwordComplexityMeter(),
          form3.passwordModified(form("newPasswd2"), trans.newPasswordAgain())(
            autocomplete := "new-password"
          ),
          form3.action(form3.submit(trans.apply()))
        )
      )
    }
}
