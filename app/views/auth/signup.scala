package views.html
package auth

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object signup {

  def apply(form: lila.security.HcaptchaForm[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.signUp.txt(),
      moreJs = frag(
        jsModule("login"),
        embedJsUnsafeLoadThen("""loginSignup.signupStart()"""),
        views.html.base.hcaptcha.script(form),
        fingerprintTag
      ),
      moreCss = cssTag("auth"),
      csp = defaultCsp.withHcaptcha.some
    ) {
      main(cls := "auth auth-signup box box-pad")(
        h1(trans.signUp()),
        postForm(
          id := "signup-form",
          cls := List(
            "form3"             -> true,
            "h-captcha-enabled" -> form.config.enabled
          ),
          action := routes.Auth.signupPost
        )(
          auth.bits.formFields(form("username"), form("password"), form("email").some, register = true),
          input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
          div(cls := "form-group text", dataIcon := "")(
            trans.computersAreNotAllowedToPlay(),
            br,
            small(
              trans.byRegisteringYouAgreeToBeBoundByOur(a(href := routes.Page.tos)(trans.termsOfService())),
              br,
              trans.readAboutOur(a(href := routes.Page.menuBookmark("privacy"))(trans.privacyPolicy())),
              br
            )
          ),
          agreement(form("agreement"), form.form.errors.exists(_.key startsWith "agreement.")),
          views.html.base.hcaptcha.tag(form),
          button(cls := "submit button text big")(trans.signUp())
        )
      )
    }

  private def agreement(form: play.api.data.Field, error: Boolean)(implicit ctx: Context) =
    div(cls := "agreement")(
      error option p(
        strong(cls := "error")(
          "You must agree to the Lichess policies listed below:"
        )
      ),
      agreements.map { case (field, i18n) =>
        form3.checkbox(form(field), i18n())
      }
    )

  private val agreements = List(
    "assistance" -> trans.agreementAssistance,
    "nice"       -> trans.agreementNice,
    "account"    -> trans.agreementAccount,
    "policy"     -> trans.agreementPolicy
  )
}
