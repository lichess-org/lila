package views.html
package auth

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object signup {

  private val recaptchaScript = raw(
    """<script src="https://www.google.com/recaptcha/api.js" async defer></script>"""
  )

  def apply(form: Form[_], recaptcha: lila.security.RecaptchaPublicConfig)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.signUp.txt(),
      moreJs = frag(
        jsTag("signup.js"),
        recaptcha.enabled option recaptchaScript,
        fingerprintTag
      ),
      moreCss = cssTag("auth"),
      csp = defaultCsp.withRecaptcha.some,
      withHrefLangs = lila.i18n.LangList.All.some
    ) {
      main(cls := "auth auth-signup box box-pad")(
        h1(trans.signUp()),
        postForm(id := "signup_form", cls := "form3", action := routes.Auth.signupPost)(
          auth.bits.formFields(form("username"), form("password"), form("email").some, register = true),
          input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
          div(cls := "form-group text", dataIcon := "")(
            trans.computersAreNotAllowedToPlay(),
            br,
            small(
              trans.byRegisteringYouAgreeToBeBoundByOur(a(href := routes.Page.tos)(trans.termsOfService()))
            )
          ),
          agreement(form("agreement"), form.errors.exists(_.key startsWith "agreement.")),
          if (recaptcha.enabled)
            button(
              cls                   := "g-recaptcha submit button text big",
              attr("data-sitekey")  := recaptcha.key,
              attr("data-callback") := "signupSubmit"
            )(trans.signUp())
          else form3.submit(trans.signUp(), icon = none, klass = "big")
        )
      )
    }

  private def agreement(form: play.api.data.Field, error: Boolean)(implicit ctx: Context) =
    div(cls := "agreement")(
      error option p(
        strong(cls := "error")(
          "You must agree to the Lishogi policies listed below:"
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
