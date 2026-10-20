package lila.web
package ui

import play.api.data.{ Field, Form }

import lila.common.HTTPRequest
import lila.core.security.HcaptchaForm
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AuthUi(helpers: Helpers):
  import helpers.{ *, given }

  def pubOrTor(using Context) =
    Page("Public proxy"):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "text", dataIcon := "2")("Ooops")),
        p("Sorry, you can't signup to Lichess through Tor or public proxies!"),
        p("You can play, train and use almost all Lichess features as an anonymous user.")
      )

  def logout(using Context) =
    Page(trans.site.logOut.txt()):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.logOut()),
        form(action := routes.Auth.logout, method := "post")(
          button(cls := "button button-red", tpe := "submit")(trans.site.logOut.txt())
        )
      )

  private def agreement(form: play.api.data.Field, error: Boolean)(using Context) =
    div(cls := "agreement")(
      error.option(p:
        strong(cls := "error"):
          "You must agree to the Lichess policies listed below:"),
      agreements.map: (field, text) =>
        form3.checkbox(form(field), text)
    )

  private def agreements(using Context) = List(
    "assistance" -> trans.site.agreementAssistance(),
    "nice"       -> trans.site.agreementNice(),
    "account" -> trans.site.agreementMultipleAccounts(a(href := routes.Cms.tos)(trans.site.termsOfService())),
    "policy"  -> trans.site.agreementPolicy()
  )

  private def formFields(username: Field, password: Field, email: Option[Field], register: Boolean)(using
      Context
  ) =
    frag(
      form3.group(
        username,
        if register then trans.site.username() else trans.site.usernameOrEmail(),
        help = register.option(trans.site.signupUsernameHint())
      ): f =>
        frag(
          form3.input(f)(autofocus, required, autocomplete := "username"),
          register.option(p(cls := "error username-exists none")(trans.site.usernameAlreadyUsed()))
        ),
      form3.passwordModified(password, trans.site.password())(
        autocomplete := (if register then "new-password" else "current-password")
      ),
      register.option(form3.passwordComplexityMeter(trans.site.newPasswordStrength())),
      email.map: email =>
        form3.group(email, trans.site.email(), help = trans.site.signupEmailHint().some)(
          form3.input(_, typ = "email")(required)
        )
    )
