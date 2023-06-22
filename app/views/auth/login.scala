package views.html
package auth

import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object login:

  import trans.tfa.*

  def apply(form: Form[?], referrer: Option[String])(using PageContext) =
    views.html.base.layout(
      title = trans.signIn.txt(),
      moreJs = jsModuleInit("login", "'login'"),
      moreCss = cssTag("auth"),
      withHrefLangs = lila.common.LangPath(routes.Auth.login).some
    ) {
      def addReferrer(url: String): String = referrer.fold(url) {
        addQueryParam(url, "referrer", _)
      }
      main(cls := "auth auth-login box box-pad")(
        h1(cls := "box__top")(trans.signIn()),
        postForm(
          cls    := "form3",
          action := addReferrer(routes.Auth.authenticate.url)
        )(
          div(cls := "one-factor")(
            if form.globalError.exists(_.messages.contains("blankedPassword")) then
              div(cls := "auth-login__blanked")(
                p(trans.blankedPassword()),
                a(href := routes.Auth.passwordReset, cls := "button button-no-upper")(trans.passwordReset())
              )
            else form3.globalError(form),
            auth.bits.formFields(form("username"), form("password"), none, register = false),
            form3.submit(trans.signIn(), icon = none),
            label(cls := "login-remember")(
              input(name := "remember", value := "true", tpe := "checkbox", checked),
              trans.rememberMe()
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
            form3.submit(trans.signIn(), icon = none)
          )
        ),
        div(cls := "alternative")(
          a(href := addReferrer(langHref(routes.Auth.signup)))(trans.signUp()),
          a(href := routes.Auth.passwordReset)(trans.passwordReset()),
          a(href := routes.Auth.magicLink)("Log in by email")
        )
      )
    }
