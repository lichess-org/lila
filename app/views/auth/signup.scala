package views.html
package auth

import controllers.routes

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.{ HTTPRequest, LangPath }

object signup:

  def apply(form: lila.security.HcaptchaForm[?])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.signUp.txt(),
      moreJs = frag(
        jsModule("login"),
        embedJsUnsafeLoadThen("""loginSignup.signupStart()"""),
        views.html.base.hcaptcha.script(form),
        fingerprintTag
      ),
      moreCss = cssTag("auth"),
      csp = defaultCsp.withHcaptcha.some,
      withHrefLangs = LangPath(routes.Auth.signup).some
    ) {
      def referrerParameter =
        HTTPRequest.queryStringGet(ctx.req, "referrer").?? { ref => s"?referrer=${urlencode(ref)}" }
      main(cls := "auth auth-signup box box-pad")(
        boxTop(trans.signUp()),
        postForm(
          id := "signup-form",
          cls := List(
            "form3"             -> true,
            "h-captcha-enabled" -> form.enabled
          ),
          action := s"${routes.Auth.signupPost}$referrerParameter"
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
      agreements.map { case (field, text) =>
        form3.checkbox(form(field), text)
      }
    )

  private def agreements(implicit ctx: Context) = List(
    "assistance" -> trans.agreementAssistance(),
    "nice"       -> trans.agreementNice(),
    "account"    -> trans.agreementMultipleAccounts(a(href := routes.Page.tos)(trans.termsOfService())),
    "policy"     -> trans.agreementPolicy()
  )
