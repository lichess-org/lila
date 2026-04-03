package lila.web
package ui

import play.api.data.Form

import lila.core.net.ValidReferrer
import lila.core.security.HcaptchaForm
import lila.core.security.SinglePostMakeToken
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AuthUi(helpers: Helpers):
  import helpers.{ *, given }

  private def addReferrer(url: String)(using referrer: Option[ValidReferrer]): String =
    referrer.fold(url)(ref => addQueryParam(url, "referrer", ref.value))

  def login(form: Form[?], isRememberMe: Boolean = true)(using
      singlePostToken: SinglePostMakeToken,
      ctx: Context
  )(using Option[ValidReferrer]) =
    val blankedPasswordError = form.globalError.exists(_.messages.contains("blankedPassword"))
    Page(trans.site.signIn.txt())
      .js(esmInit("bits.login", "login"))
      .css("bits.auth")
      .hrefLangs(lila.ui.LangPath(routes.Auth.login)):
        main(cls := "auth auth-login box box-pad")(
          authTabs("login"),
          postForm(
            cls := "form3",
            action := addReferrer(routes.Auth.authenticate.url)
          )(
            div(cls := "one-factor")(
              blankedPasswordError.option:
                div(cls := "auth-login__blanked")(
                  p(trans.site.blankedPassword()),
                  a(href := routes.Auth.passwordReset, cls := "button button-no-upper")(
                    trans.site.passwordReset()
                  )
                )
              ,
              form3.group(form("username"), trans.site.usernameOrEmail()): f =>
                div(cls := "text-wrapper")(
                  form3.input(f)(autofocus, required, autocomplete := "username"),
                  clearFieldButton
                ),
              form3.passwordModified(form("password"), trans.site.password())(
                autocomplete := "current-password"
              ),
              div(cls := "password-reset")(
                a(href := routes.Auth.passwordReset)(trans.site.passwordReset())
              ),
              div(cls := "login-remember form-check__container")(
                form3.nativeCheckbox(
                  "login-remember-me",
                  "remember",
                  checked = isRememberMe
                ),
                label(cls := "form-label", `for` := "login-remember-me")(
                  trans.site.rememberMe()
                )
              ),
              form3.hidden(form("singlePost"), singlePostToken(using ctx.req).some),
              form3.errors(form("singlePost")),
              form3.submit(trans.site.signIn(), icon = none),
              authGlobalError(form).ifFalse(blankedPasswordError)
            ),
            div(cls := "two-factor none")(
              form3.group(
                form("token"),
                trans.tfa.authenticationCode(),
                help = Some(span(dataIcon := Icon.PhoneMobile)(trans.tfa.openTwoFactorApp()))
              )(
                form3.input(_)(
                  attr("inputmode") := "numeric",
                  autocomplete := "one-time-code",
                  pattern := "[0-9]{6}"
                )
              ),
              p(cls := "error none")("Invalid code."),
              form3.submit(trans.site.signIn(), icon = none)
            )
          ),
          div(cls := "or-separator")(span(trans.site.orSeparator())),
          a(href := addReferrer(routes.Auth.magicLink.url), cls := "button button-empty magic-link")(
            trans.site.logInByEmail()
          )
        )

  def signup(form: lila.core.security.HcaptchaForm[?], simple: Boolean)(using
      singlePostToken: SinglePostMakeToken,
      ctx: Context
  )(using Option[ValidReferrer]) =
    Page(trans.site.signUp.txt())
      .js(esmInit("bits.login", "signup"))
      .js(hcaptchaScript(form))
      .js(fingerprintTag)
      .css("bits.auth")
      .csp(_.withHcaptcha)
      .hrefLangs(lila.ui.LangPath(routes.Auth.signup)):
        main(
          cls := List(
            "auth auth-signup box box-pad" -> true,
            "auth-signup--simple" -> simple
          )
        )(
          authTabs("signup"),
          postForm(
            id := "signup-form",
            cls := List(
              "form3" -> true,
              "h-captcha-enabled" -> form.enabled
            ),
            action := addReferrer(routes.Auth.signupPost.url),
            autocomplete := "off"
          )(
            form3.group(
              form("username"),
              trans.site.username(),
              help = simple.not.option(trans.site.signupUsernameHint())
            ): f =>
              frag(
                div(cls := "text-wrapper")(
                  form3.input(f)(autofocus, required, autocomplete := "username"),
                  clearFieldButton
                ),
                p(cls := "error username-exists none")(trans.site.usernameAlreadyUsed())
              ),
            form3.group(form("password"), trans.site.password()): f =>
              frag(
                div(cls := "password-wrapper")(
                  form3.input(f, typ = "password")(required)(autocomplete := "new-password"),
                  form3.passwordRevealButton
                ),
                simple.option:
                  div(cls := "password-generator")(button("Generate a random password"))
                ,
                form3.passwordComplexityMeter(trans.site.newPasswordStrength())
              ),
            form3.group(
              form("email"),
              trans.site.email(),
              help = simple.not.option(trans.site.signupEmailPromise())
            ): f =>
              div(cls := "text-wrapper")(
                form3.input(f, typ = "email")(required),
                clearFieldButton
              ),
            input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
            simple.not.option:
              div(cls := "form-group text", dataIcon := Icon.InfoCircle)(
                trans.site.computersAreNotAllowedToPlay(),
                br,
                small(
                  tosLink,
                  br,
                  trans.site.readAboutOur(
                    a(href := routes.Cms.menuPage(lila.core.id.CmsPageKey("privacy")))(
                      trans.site.privacyPolicy()
                    )
                  ),
                  br
                )
              )
            ,
            agreement(form("agreement"), form.form.errors.exists(_.key.startsWith("agreement."))),
            lila.ui.bits.hcaptcha(form),
            button(cls := "submit button", tpe := "submit")(trans.site.signUp()),
            simple.option:
              small(cls := "form-help")(tosLink)
            ,
            form3.hidden(form("singlePost"), singlePostToken(using ctx.req).some),
            form3.errors(form("singlePost")),
            authGlobalError(form.form)
          )
        )

  private def tosLink(using Translate) = trans.site.byRegisteringYouAgreeToBeBoundByOur(
    a(href := routes.Cms.tos)(trans.site.termsOfService())
  )

  def checkYourEmail(
      email: Option[EmailAddress],
      form: Option[Form[?]] = None
  )(using Context, Option[ValidReferrer]) =
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
                postForm(action := addReferrer(routes.Auth.fixEmail.url))(
                  input(
                    id := "new-email",
                    tpe := "email",
                    required,
                    name := "email",
                    value := form.flatMap(_("email").value).getOrElse(email.value),
                    pattern := s"^((?!^${email.value}$$).)*$$",
                    size := 40
                  ),
                  " ",
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

  def signupConfirm(user: User, token: String)(using Context, Option[ValidReferrer]) =
    Page(trans.site.signUp.txt())
      .css("bits.email-confirm"):
        main(cls := "page-small box box-pad signup-confirm")(
          h1(iconFlair(Flair("activity.party-popper")), trans.onboarding.welcomeToLichess()),
          postForm(action := addReferrer(routes.Auth.signupConfirmEmailPost(token).url)):
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
        main(cls := "auth page-small box box-pad")(
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

  def magicLink(form: HcaptchaForm[?], fail: Boolean)(using Context, Option[ValidReferrer]) =
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
          postForm(cls := "form3", action := addReferrer(routes.Auth.magicLinkApply.url))(
            form3.group(form("email"), trans.site.email())(
              form3.input(_, typ = "email")(autofocus, required, autocomplete := "email")
            ),
            lila.ui.bits.hcaptcha(form),
            form3.action(form3.submit(trans.site.emailMeALink()))
          )
        )

  def magicLinkSent(using Context) =
    Page(trans.site.logInByEmail.txt()):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)(trans.site.checkYourEmail())),
        p(trans.site.sentEmailWithLink()),
        p(trans.site.ifYouDoNotSeeTheEmailCheckOtherPlaces())
      )

  def tokenLoginConfirmation(user: User, token: String)(using Context, Option[ValidReferrer]) =
    Page(s"Log in as ${user.username}").css("bits.form3"):
      main(cls := "page-small box box-pad")(
        boxTop(h1("Log in as ", userLink(user))),
        postForm(action := addReferrer(routes.Auth.loginWithTokenPost(token).url))(
          form3.actions(
            a(href := routes.Lobby.home)(trans.site.cancel()),
            submitButton(cls := "button")(s"${user.username} is my Lichess username, log me in")
          )
        )
      )

  def checkYourEmailBanner(user: UserName, email: EmailAddress) =
    div(cls := "email-confirm-banner")(
      span(s"Almost there, ${user}! Now check your email (${email.conceal}) for signup confirmation."),
      a(href := routes.Auth.checkYourEmail)("Need help?")
    )

  def pubOrTor =
    Page("Public proxy"):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "text")("Ooops")),
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

  private def authTabs(active: String)(using Context, Option[ValidReferrer]) =
    div(cls := "auth-tabs")(
      a(href := addReferrer(langHref(routes.Auth.login)), cls := (active == "login").option("active"))(
        trans.site.signIn()
      ),
      a(href := addReferrer(langHref(routes.Auth.signup)), cls := (active == "signup").option("active"))(
        trans.site.signUp()
      )
    )

  private def agreement(form: play.api.data.Field, error: Boolean)(using Context) =
    div(cls := "agreement")(
      error.option(p:
        strong(cls := "error"):
          "You must agree to the Lichess policies listed below:"),
      agreements.map: (field, text) =>
        form3.checkboxGroup(form(field), text)
    )

  private def agreements(using Context) = List(
    "assistance" -> trans.site.agreementAssistance(),
    "nice" -> trans.site.agreementNice(),
    "account" -> trans.site.agreementMultipleAccounts(a(href := routes.Cms.tos)(trans.site.termsOfService()))
  )

  private def authGlobalError(form: Form[?])(using Translate): Option[Frag] =
    form.globalError.map: err =>
      div(cls := "form-group is-invalid auth-global-error")(
        span(cls := "text", dataIcon := Icon.CautionCircle)(transKey(trans(err.message), err.args))
      )

  private def clearFieldButton(using Context) =
    button(
      cls := "text-clear",
      tpe := "button",
      dataIcon := Icon.Cancel,
      title := trans.site.clearField.txt(),
      aria.label := trans.site.clearField.txt()
    )
