package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object close {

  def apply(u: lila.user.User, form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.closeAccount.txt()}",
    active = "close"
  ) {
    div(cls := "account box box-pad")(
      h1(dataIcon := "j", cls := "text")(trans.closeAccount()),
      st.form(cls := "form3", action := routes.Account.closeConfirm, method := "POST")(
        div(cls := "form-group")(trans.closeAccountExplanation()),
        div(cls := "form-group")("You will not be allowed to open a new account with the same name, even if the case if different."),
        form3.passwordModified(form("passwd"), trans.password())(autocomplete := "off"),
        form3.actions(frag(
          a(href := routes.User.show(u.username))(trans.changedMindDoNotCloseAccount()),
          form3.submit(
            trans.closeAccount(),
            icon = "j".some,
            confirm = "Closing is definitive. There is no going back. Are you sure?".some,
            klass = "button-red"
          )
        ))
      )
    )
  }
}
