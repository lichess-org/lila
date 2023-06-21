package views.html
package auth

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.{ HTTPRequest, LangPath }
import lila.security.PasswordCheck

object signup:

  def apply(form: lila.security.HcaptchaForm[?])(using ctx: PageContext) =
    views.html.base.layout(
      title = trans.signUp.txt(),
      moreJs = frag(
        jsModuleInit("login", "'signup'"),
        views.html.base.hcaptcha.script(form),
        fingerprintTag
      ),
      moreCss = cssTag("auth"),
      csp = defaultCsp.withHcaptcha.some,
      withHrefLangs = LangPath(routes.Auth.signup).some
    ) {
      main(cls := "auth auth-signup box box-pad")(
        h1(cls := "box__top")(trans.signUp()),
        postForm(
          id := "signup-form",
          cls := List(
            "form3"             -> true,
            "h-captcha-enabled" -> form.enabled
          ),
          action := HTTPRequest.queryStringGet(ctx.req, "referrer").foldLeft(routes.Auth.signupPost.url) {
            (url, ref) => addQueryParam(url, "referrer", ref)
          }
        )(
          auth.bits.formFields(form("username"), form("password"), form("email").some, register = true),
          globalErrorNamed(form.form, PasswordCheck.errorSame),
          input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
          div(cls := "form-group text", dataIcon := licon.InfoCircle)(
            trans.computersAreNotAllowedToPlay(),
            br,
            small(
              trans.byRegisteringYouAgreeToBeBoundByOur(
                a(href := routes.ContentPage.tos)(trans.termsOfService())
              ),
              br,
              trans.readAboutOur(
                a(href := routes.ContentPage.menuBookmark("privacy"))(trans.privacyPolicy())
              ),
              br
            )
          ),
          agreement(form("agreement"), form.form.errors.exists(_.key startsWith "agreement.")),
          views.html.base.hcaptcha.tag(form),
          button(cls := "submit button text big")(trans.signUp())
        )
      )
    }

  private def agreement(form: play.api.data.Field, error: Boolean)(using PageContext) =
    div(cls := "agreement")(
      error option p(
        strong(cls := "error")(
          "You must agree to the Lichess policies listed below:"
        )
      ),
      agreements.map: (field, text) =>
        form3.checkbox(form(field), text)
    )

  private def agreements(using PageContext) = List(
    "assistance" -> trans.agreementAssistance(),
    "nice"       -> trans.agreementNice(),
    "account" -> trans.agreementMultipleAccounts(a(href := routes.ContentPage.tos)(trans.termsOfService())),
    "policy"  -> trans.agreementPolicy()
  )
