package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object kid {

  def apply(u: lila.user.User, form: play.api.data.Form[_], managed: Boolean)(implicit ctx: Context) =
    account.layout(
      title = s"${u.username} - ${trans.kidMode.txt()}",
      active = "kid"
    ) {
      div(cls := "account box box-pad")(
        h1(trans.kidMode()),
        standardFlash(),
        p(trans.kidModeExplanation()),
        br,
        br,
        br,
        if (managed)
          p("Your account is managed. Ask your chess teacher about lifting kid mode.")
        else
          postForm(cls := "form3", action := s"${routes.Account.kidPost}?v=${!u.kid}")(
            form3.passwordModified(form("passwd"), trans.password())(autofocus, autocomplete := "off"),
            submitButton(
              cls := List(
                "button"     -> true,
                "button-red" -> u.kid
              )
            )(if (u.kid) trans.disableKidMode.txt() else trans.enableKidMode.txt())
          ),
        br,
        br,
        p(trans.inKidModeTheLichessLogoGetsIconX(span(cls := "kiddo", title := trans.kidMode.txt())(":)")))
      )
    }
}
