package views.html
package account

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object close:

  import trans.settings.*

  def apply(u: lila.user.User, form: play.api.data.Form[?], managed: Boolean)(using Context) =
    account.layout(
      title = s"${u.username} - ${closeAccount.txt()}",
      active = "close"
    ) {
      div(cls := "account box box-pad")(
        boxTop(h1(cls := "text", dataIcon := "")(closeAccount())),
        if (managed)
          p(managedAccountCannotBeClosed())
        else
          postForm(cls := "form3", action := routes.Account.closeConfirm)(
            div(cls := "form-group")(closeAccountExplanation()),
            div(cls := "form-group")(cantOpenSimilarAccount()),
            form3.passwordModified(form("passwd"), trans.password())(autofocus, autocomplete := "off"),
            form3.actions(
              frag(
                a(href := routes.User.show(u.username))(changedMindDoNotCloseAccount()),
                form3.submit(
                  closeAccount(),
                  icon = "".some,
                  confirm = closingIsDefinitive.txt().some
                )(cls := "button-red")
              )
            )
          )
      )
    }
