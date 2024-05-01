package views.auth

import lila.app.templating.Environment.{ *, given }

import lila.common.HTTPRequest
import lila.security.PasswordCheck

object signup:

  def apply(form: lila.core.security.HcaptchaForm[?])(using ctx: PageContext) =
    views.base.layout(
      title = trans.site.signUp.txt(),
      modules = jsModuleInit("bits.login", "signup") ++ hcaptchaScript(form),
      moreJs = frag(fingerprintTag),
      moreCss = cssTag("auth"),
      csp = defaultCsp.withHcaptcha.some,
      withHrefLangs = lila.ui.LangPath(routes.Auth.signup).some
    ) {
      main(cls := "auth auth-signup box box-pad")(
        h1(cls := "box__top")(trans.site.signUp()),
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
          bits.formFields(form("username"), form("password"), form("email").some, register = true),
          globalErrorNamed(form.form, PasswordCheck.errorSame),
          input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
          div(cls := "form-group text", dataIcon := Icon.InfoCircle)(
            trans.site.computersAreNotAllowedToPlay(),
            br,
            small(
              trans.site.byRegisteringYouAgreeToBeBoundByOur(
                a(href := routes.Cms.tos)(trans.site.termsOfService())
              ),
              br,
              trans.site.readAboutOur(
                a(href := routes.Cms.menuPage("privacy"))(trans.site.privacyPolicy())
              ),
              br
            )
          ),
          agreement(form("agreement"), form.form.errors.exists(_.key.startsWith("agreement."))),
          lila.ui.bits.hcaptcha(form),
          button(cls := "submit button text big")(trans.site.signUp())
        )
      )
    }

  private def agreement(form: play.api.data.Field, error: Boolean)(using Context) =
    div(cls := "agreement")(
      error.option(p:
        strong(cls := "error"):
          "You must agree to the Lichess policies listed below:"
      ),
      agreements.map: (field, text) =>
        form3.checkbox(form(field), text)
    )

  private def agreements(using Context) = List(
    "assistance" -> trans.site.agreementAssistance(),
    "nice"       -> trans.site.agreementNice(),
    "account" -> trans.site.agreementMultipleAccounts(a(href := routes.Cms.tos)(trans.site.termsOfService())),
    "policy"  -> trans.site.agreementPolicy()
  )
