package views.html
package auth

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object login:

  import trans.tfa.*

  def apply(form: Form[?], referrer: Option[String])(using PageContext) =
    views.html.base.layout(
      title = trans.site.signIn.txt(),
      modules = jsModuleInit("bits.login", "login"),
      moreCss = cssTag("auth"),
      withHrefLangs = lila.core.app.LangPath(routes.Auth.login).some
    ) {
      def addReferrer(url: String): String = referrer.fold(url) {
        addQueryParam(url, "referrer", _)
      }
      main(cls := "auth auth-login box box-pad")(
        h1(cls := "box__top")(trans.site.signIn()),
        postForm(
          cls    := "form3",
          action := addReferrer(routes.Auth.authenticate.url)
        )(
          div(cls := "one-factor")(
            if form.globalError.exists(_.messages.contains("blankedPassword")) then
              div(cls := "auth-login__blanked")(
                p(trans.site.blankedPassword()),
                a(href := routes.Auth.passwordReset, cls := "button button-no-upper")(
                  trans.site.passwordReset()
                )
              )
            else form3.globalError(form),
            auth.bits.formFields(form("username"), form("password"), none, register = false),
            form3.submit(trans.site.signIn(), icon = none),
            label(cls := "login-remember")(
              input(name := "remember", value := "true", tpe := "checkbox", checked),
              trans.site.rememberMe()
            )
          ),
          div(cls := "two-factor none")(
            form3.group(
              form("token"),
              authenticationCode(),
              help = Some(span(dataIcon := licon.PhoneMobile)(openTwoFactorApp()))
            )(
              form3.input(_)(autocomplete := "one-time-code", pattern := "[0-9]{6}")
            ),
            p(cls := "error none")("Invalid code."),
            form3.submit(trans.site.signIn(), icon = none)
          )
        ),
        div(cls := "alternative")(
          a(href := addReferrer(langHref(routes.Auth.signup)))(trans.site.signUp()),
          a(href := routes.Auth.passwordReset)(trans.site.passwordReset()),
          a(href := routes.Auth.magicLink)("Log in by email")
        )
      )
    }
