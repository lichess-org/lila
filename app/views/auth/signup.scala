package views.html
package auth

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object signup {

  def apply(form: lila.security.RecaptchaForm[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.signUp.txt(),
      moreJs = frag(
        jsModule("login"),
        embedJsUnsafeLoadThen("""loginSignup.signupStart()"""),
        views.html.base.recaptcha.script(form),
        fingerprintTag,
        embedJsUnsafeLoadThen("""
          lichess.loadModule('passwordComplexity').then(() =>
            passwordComplexity.addPasswordChangeListener('form3-password')
          )""")
      ),
      moreCss = cssTag("auth"),
      csp = defaultCsp.withRecaptcha.some
    ) {
      main(cls := "auth auth-signup box box-pad")(
        h1(trans.signUp()),
        postForm(id := form.formId, cls := "form3", action := routes.Auth.signupPost())(
          auth.bits.formFields(form("username"), form("password"), form("email").some, register = true),
          input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
          div(cls := "form-group text", dataIcon := "")(
            trans.computersAreNotAllowedToPlay(),
            br,
            small(
              trans.byRegisteringYouAgreeToBeBoundByOur(a(href := routes.Page.tos())(trans.termsOfService())),
              br,
              trans.readAboutOur(a(href := routes.Page.menuBookmark("privacy"))(trans.privacyPolicy())),
              br
            )
          ),
          agreement(form("agreement"), form.form.errors.exists(_.key startsWith "agreement.")),
          views.html.base.recaptcha.button(form) {
            button(cls := "submit button text big")(trans.signUp())
          }
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
