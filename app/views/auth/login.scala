package views.html
package auth

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object login {

  val twoFactorHelp = span(dataIcon := "")(
    "Open the two-factor authentication app on your device to view your authentication code and verify your identity."
  )

  def apply(form: Form[_], referrer: Option[String])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.signIn.txt(),
      moreJs = frag(
        jsModule("login"),
        embedJsUnsafeLoadThen("""loginSignup.loginStart()""")
      ),
      moreCss = cssTag("auth")
    ) {
      main(cls := "auth auth-login box box-pad")(
        h1(trans.signIn()),
        postForm(
          cls := "form3",
          action := s"${routes.Auth.authenticate()}${referrer.?? { ref =>
            s"?referrer=${urlencode(ref)}"
          }}"
        )(
          div(cls := "one-factor")(
            form3.globalError(form),
            auth.bits.formFields(form("username"), form("password"), none, register = false),
            form3.submit(trans.signIn(), icon = none)
          ),
          div(cls := "two-factor none")(
            form3.group(form("token"), raw("Authentication code"), help = Some(twoFactorHelp))(
              form3.input(_)(autocomplete := "one-time-code", pattern := "[0-9]{6}")
            ),
            p(cls := "error none")("Invalid code."),
            form3.submit(trans.signIn(), icon = none)
          )
        ),
        div(cls := "alternative")(
          a(href := routes.Auth.signup())(trans.signUp()),
          a(href := routes.Auth.passwordReset())(trans.passwordReset()),
          a(href := routes.Auth.magicLink())("Log in by email")
        )
      )
    }
}
