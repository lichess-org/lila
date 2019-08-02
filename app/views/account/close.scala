package views.html
package account

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object close {

  def apply(u: lidraughts.user.User, form: play.api.data.Form[_])(implicit ctx: Context) = account.layout(
    title = s"${u.username} - ${trans.closeAccount.txt()}",
    active = "close"
  ) {
    div(cls := "account box box-pad")(
      h1(dataIcon := "j", cls := "text")(trans.closeAccount()),
      postForm(cls := "form3", action := routes.Account.closeConfirm)(
        div(cls := "form-group")(trans.closeAccountExplanation()),
        div(cls := "form-group")(trans.noNewAccountWithSameName()),
        form3.passwordModified(form("passwd"), trans.password())(autocomplete := "off"),
        form3.actions(frag(
          a(href := routes.User.show(u.username))(trans.changedMindDoNotCloseAccount()),
          form3.submit(
            trans.closeAccount(),
            icon = "j".some,
            confirm = trans.closingIsDefinitive.txt().some,
            klass = "button-red"
          )
        ))
      )
    )
  }
}
