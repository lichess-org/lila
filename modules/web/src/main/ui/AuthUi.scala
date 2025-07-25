package lila.web
package ui

import play.api.data.{ Field, Form }

import lila.common.HTTPRequest
import lila.core.security.HcaptchaForm
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AuthUi(helpers: Helpers):
  import helpers.{ *, given }

  def login(form: Form[?], referrer: Option[String], isRememberMe: Boolean = true)(using Context) =
    def addReferrer(url: String): String = referrer.fold(url)(addQueryParam(url, "referrer", _))
    Page(trans.site.signIn.txt())
      .js(esmInit("bits.login", "login"))
      .css("bits.auth")
      .hrefLangs(lila.ui.LangPath(routes.Auth.login)):
        main(cls := "auth auth-login box box-pad")(
          h1(cls := "box__top")(trans.site.signIn()),
          postForm(
            cls := "form3",
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
              formFields(form("username"), form("password"), none, register = false),
              form3.submit(trans.site.signIn(), icon = none),
              label(cls := "login-remember")(
                input(
                  name := "remember",
                  value := "true",
                  tpe := "checkbox",
                  isRememberMe.option(checked)
                ),
                trans.site.rememberMe()
              )
            ),
            div(cls := "two-factor none")(
              form3.group(
                form("token"),
                trans.tfa.authenticationCode(),
                help = Some(span(dataIcon := Icon.PhoneMobile)(trans.tfa.openTwoFactorApp()))
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

  def signup(form: lila.core.security.HcaptchaForm[?])(using ctx: Context) =
    Page(trans.site.signUp.txt())
      .js(esmInit("bits.login", "signup"))
      .js(hcaptchaScript(form))
      .js(fingerprintTag)
      .css("bits.auth")
      .csp(_.withHcaptcha)
      .hrefLangs(lila.ui.LangPath(routes.Auth.signup)):
        main(cls := "auth auth-signup box box-pad")(
          h1(cls := "box__top")(trans.site.signUp()),
          postForm(
            id := "signup-form",
            cls := List(
              "form3" -> true,
              "h-captcha-enabled" -> form.enabled
            ),
            action := HTTPRequest.queryStringGet(ctx.req, "referrer").foldLeft(routes.Auth.signupPost.url) {
              (url, ref) => addQueryParam(url, "referrer", ref)
            }
          )(
            formFields(form("username"), form("password"), form("email").some, register = true),
            globalErrorNamed(form.form, "error.namePassword"),
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
                  a(href := routes.Cms.menuPage(lila.core.id.CmsPageKey("privacy")))(
                    trans.site.privacyPolicy()
                  )
                ),
                br
              )
            ),
            agreement(form("agreement"), form.form.errors.exists(_.key.startsWith("agreement."))),
            lila.ui.bits.hcaptcha(form),
            button(cls := "submit button text big")(trans.site.signUp())
          )
        )

  def checkYourEmail(
      email: Option[EmailAddress],
      form: Option[Form[?]] = None
  )(using ctx: Context) =
    Page("Check your email")
      .css("bits.email-confirm")
      .js(esmInitBit("validateEmail")):
        main(
          cls := s"page-small box box-pad email-confirm ${
              if form.exists(_.hasErrors) then "error" else "anim"
            }"
        )(
          boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)(trans.site.checkYourEmail())),
          p(trans.site.weHaveSentYouAnEmailClickTheLink()),
          h2("Not receiving it?"),
          ol(
            li(h3(trans.site.ifYouDoNotSeeTheEmailCheckOtherPlaces())),
            email.map { email =>
              li(
                h3("Make sure your email address is correct:"),
                br,
                br,
                postForm(action := routes.Auth.fixEmail)(
                  input(
                    id := "new-email",
                    tpe := "email",
                    required,
                    name := "email",
                    value := form.flatMap(_("email").value).getOrElse(email.value),
                    pattern := s"^((?!^${email.value}$$).)*$$"
                  ),
                  submitButton(cls := "button")("Change it"),
                  form.map: f =>
                    errMsg(f("email"))
                )
              )
            },
            li(
              h3("Wait up to 5 minutes."),
              br,
              "Depending on your email provider, it can take a while to arrive."
            ),
            li(
              h3("Still not getting it?"),
              br,
              "Did you make sure your email address is correct?",
              br,
              "Did you wait 5 minutes?",
              br,
              "If so, ",
              a(href := routes.Account.emailConfirmHelp)("proceed to this page to solve the issue"),
              "."
            )
          )
        )

  def signupConfirm(user: User, token: String)(using Context) =
    Page(trans.site.signUp.txt())
      .css("bits.email-confirm"):
        main(cls := "page-small box box-pad signup-confirm")(
          h1(iconFlair(Flair("activity.party-popper")), trans.onboarding.welcomeToLichess()),
          postForm(action := routes.Auth.signupConfirmEmailPost(token)):
            submitButton(cls := "button button-fat button-no-upper")(
              trans.onboarding.logInAsUsername(user.username)
            )
        )

  def passwordReset(form: HcaptchaForm[?], fail: Option[String])(using Context) =
    Page(trans.site.passwordReset.txt())
      .css("bits.auth")
      .js(hcaptchaScript(form))
      .csp(_.withHcaptcha):
        main(cls := "auth auth-signup box box-pad")(
          boxTop(
            h1(
              fail.isDefined.option(span(cls := "is-red", dataIcon := Icon.X)),
              trans.site.passwordReset()
            )
          ),
          postForm(cls := "form3", action := routes.Auth.passwordResetApply)(
            fail.map(p(cls := "error")(_)),
            form3.group(form("email"), trans.site.email())(
              form3.input(_, typ = "email")(autofocus, required, autocomplete := "email")
            ),
            lila.ui.bits.hcaptcha(form),
            form3.action(form3.submit(trans.site.emailMeALink()))
          )
        )

  def passwordResetSent(email: String)(using Context) =
    Page(trans.site.passwordReset.txt()).css("bits.auth"):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)(trans.site.checkYourEmail())),
        p(trans.site.weHaveSentYouAnEmailTo(email)),
        p(trans.site.ifYouDoNotGetTheEmail()),
        ul(cls := "checklist")(
          li(trans.site.checkAllEmailFolders()),
          li(trans.site.verifyYourAddress(email))
        )
      )

  def passwordResetConfirm(token: String, form: Form[?], ok: Option[Boolean] = None)(using
      Context
  )(using me: Me) =
    Page(s"${me.username} - ${trans.site.changePassword.txt()}")
      .css("bits.auth")
      .js(esmInit("bits.login", "reset")):
        main(cls := "page-small box box-pad")(
          boxTop(
            (ok match
              case Some(true) => h1(cls := "is-green text", dataIcon := Icon.Checkmark)
              case Some(false) => h1(cls := "is-red text", dataIcon := Icon.X)
              case _ => h1
            )(
              userLink(me, withOnline = false),
              " - ",
              trans.site.changePassword()
            )
          ),
          postForm(cls := "form3", action := routes.Auth.passwordResetConfirmApply(token))(
            form3.hidden(form("token")),
            form3.passwordModified(form("newPasswd1"), trans.site.newPassword())(
              autofocus,
              autocomplete := "new-password"
            ),
            form3.passwordComplexityMeter(trans.site.newPasswordStrength()),
            form3.passwordModified(form("newPasswd2"), trans.site.newPasswordAgain())(
              autocomplete := "new-password"
            ),
            form3.globalError(form),
            form3.action(form3.submit(trans.site.changePassword()))
          )
        )

  def magicLink(form: HcaptchaForm[?], fail: Boolean)(using Context) =
    Page("Log in by email")
      .css("bits.auth")
      .js(hcaptchaScript(form))
      .csp(_.withHcaptcha):
        main(cls := "auth auth-signup box box-pad")(
          boxTop(
            h1(
              fail.option(span(cls := "is-red", dataIcon := Icon.X)),
              "Log in by email"
            )
          ),
          p("We will send you an email containing a link to log you in."),
          postForm(cls := "form3", action := routes.Auth.magicLinkApply)(
            form3.group(form("email"), trans.site.email())(
              form3.input(_, typ = "email")(autofocus, required, autocomplete := "email")
            ),
            lila.ui.bits.hcaptcha(form),
            form3.action(form3.submit(trans.site.emailMeALink()))
          )
        )

  def magicLinkSent(using Context) =
    Page("Log in by email"):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)(trans.site.checkYourEmail())),
        p("We've sent you an email with a link."),
        p(trans.site.ifYouDoNotSeeTheEmailCheckOtherPlaces())
      )

  def tokenLoginConfirmation(user: User, token: String, referrer: Option[String])(using Context) =
    Page(s"Log in as ${user.username}").css("bits.form3"):
      main(cls := "page-small box box-pad")(
        boxTop(h1("Log in as ", userLink(user))),
        postForm(action := routes.Auth.loginWithTokenPost(token, referrer))(
          form3.actions(
            a(href := routes.Lobby.home)(trans.site.cancel()),
            submitButton(cls := "button")(s"${user.username} is my Lichess username, log me in")
          )
        )
      )

  def checkYourEmailBanner(user: UserName, email: EmailAddress) =
    div(cls := "email-confirm-banner")(
      s"Almost there, ${user}! Now check your email (${email.conceal}) for signup confirmation.",
      a(href := routes.Auth.checkYourEmail)("Need help?")
    )

  def pubOrTor =
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
    "nice" -> trans.site.agreementNice(),
    "account" -> trans.site.agreementMultipleAccounts(a(href := routes.Cms.tos)(trans.site.termsOfService())),
    "policy" -> trans.site.agreementPolicy()
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
