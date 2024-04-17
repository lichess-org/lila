package views.html
package account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }

object close:

  import trans.settings.*

  def apply(form: play.api.data.Form[?], managed: Boolean)(using PageContext)(using me: Me) =
    account.layout(
      title = s"${me.username} - ${closeAccount.txt()}",
      active = "close"
    ):
      div(cls := "box box-pad")(
        boxTop(h1(cls := "text", dataIcon := Icon.CautionCircle)(closeAccount())),
        if managed then p(managedAccountCannotBeClosed())
        else
          postForm(cls := "form3", action := routes.Account.closeConfirm)(
            div(cls := "form-group")(closeAccountExplanation()),
            div(cls := "form-group")(cantOpenSimilarAccount()),
            form3.passwordModified(form("passwd"), trans.site.password())(autofocus, autocomplete := "off"),
            form3.actions(
              frag(
                a(href := routes.User.show(me.username))(changedMindDoNotCloseAccount()),
                form3.submit(
                  closeAccount(),
                  icon = Icon.CautionCircle.some,
                  confirm = closingIsDefinitive.txt().some
                )(cls := "button-red")
              )
            )
          )
      )
