package lila.web
package ui

import play.api.data.Form

import lila.core.net.ValidReferrer
import lila.core.security.TurnstilePublicConfig
import lila.core.misc.AuthCustomUi
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class AuthUi(helpers: Helpers):
  import helpers.{ *, given }

  private def addReferrer(url: String)(using referrer: Option[ValidReferrer]): String =
    referrer.fold(url)(ref => addQueryParam(url, "referrer", ref.value))

  private val takex3OpenAppLichessLoginUrl =
    "https://taketaketake.com/open-app?target=%2F%3FnewGame%3Dtrue%26lichessLogin%3Dtrue"

  private def authClasses(base: String, custom: Option[AuthCustomUi]) =
    List(base -> true) ::: custom.map(c => s"auth--${c.cssClass}" -> true).toList

  private def logoOrConnection(using custom: Option[AuthCustomUi]) = custom match
    case None =>
      div(cls := "auth__brand")(
        span(cls := "auth__brand__logo", aria.hidden := "true"),
        span(cls := "auth__brand__name")("lichess.org")
      )
    case Some(c) =>
      frag(customLogo(c), h2(cls := "oauth__connection__to-lichess")("Connect to Lichess"))

  private def takex3ConnectionHeader(title: String)(using custom: Option[AuthCustomUi]) = custom match
    case Some(c) => frag(customLogo(c), h1(cls := "auth__takex3-page-title")(title))
    case None => h1(cls := "auth__takex3-page-title")(title)

  def customLogo(c: AuthCustomUi) =
    val takex3 = c.cssClass == "takex3"
    div(cls := "oauth__connection")(
      img(
        src := assetUrl(c.imagePath),
        alt := c.name,
        cls := "oauth__connection__service"
      ),
      div(cls := "oauth__connection__between")(
        takex3.option(takex3ConnectionDash),
        if takex3 then takex3ConnectionCheck else iconTag(Icon.Checkmark)(cls := "oauth__connection__check"),
        takex3.option(takex3ConnectionDash)
      ),
      lila.web.ui.bits.logo
    )

  private def takex3ConnectionDash =
    span(cls := "oauth__connection__dash-pad")(
      raw(
        """<svg class="oauth__connection__dash" width="36" height="2" viewBox="0 0 36 2" fill="none" xmlns="http://www.w3.org/2000/svg"><rect width="3" height="2" fill="#00A9A9"/><rect x="9" width="6" height="2" fill="#00A9A9"/><rect x="21" width="6" height="2" fill="#00A9A9"/><rect x="33" width="3" height="2" fill="#00A9A9"/></svg>"""
      )
    )

  private val takex3CheckSvg =
    """<svg width="17" height="17" viewBox="0 0 17 17" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M6.19758 13.5645L15.3185 4.56042L14.0323 3.27413L6.19758 11.1088L3.04032 7.83461L1.75403 9.12091L6.19758 13.5645Z" fill="white"/></svg>"""

  private def takex3CheckIcon(klass: String) =
    span(cls := klass)(
      raw(
        takex3CheckSvg
      )
    )

  private def takex3ConnectionCheck = takex3CheckIcon("oauth__connection__check")

  def login(form: Form[?], isRememberMe: Boolean = true)(using
      TurnstilePublicConfig,
      Option[ValidReferrer]
  )(using ctx: Context, custom: Option[AuthCustomUi]) =
    given Translate = oauthClientLanguage
    val blankedPasswordError = form.globalError.exists(_.messages.contains("blankedPassword"))
    Page(trans.site.signIn.txt())
      .js(esmInit("bits.auth", "login"))
      .css("bits.auth")
      .csp(_.withTurnstile)
      .hrefLangs(lila.ui.LangPath(routes.Auth.login)):
        main(cls := "auth auth-login box box-pad")(
          logoOrConnection,
          authTabs("login"),
          postForm(
            cls := "form3",
            action := addReferrer(routes.Auth.authenticate.url)
          )(
            div(cls := "one-factor")(
              if blankedPasswordError then
                div(cls := "auth-login__blanked")(
                  p(trans.site.blankedPassword()),
                  a(href := routes.Auth.passwordReset, cls := "button button-no-upper")(
                    trans.site.passwordReset()
                  )
                )
              else authGlobalError(form),
              form3.group(form("username"), trans.site.usernameOrEmail()): f =>
                div(cls := "text-wrapper")(
                  form3.input(f)(autofocus, required, autocomplete := "username", testId("username")),
                  clearFieldButton
                ),
              form3.passwordModified(form("password"), trans.site.password())(
                autocomplete := "current-password",
                testId("password")
              ),
              div(cls := "password-reset")(
                a(href := routes.Auth.passwordReset)(trans.site.passwordReset())
              ),
              if custom.isEmpty then
                div(cls := "login-remember form-check__container")(
                  form3.nativeCheckbox(
                    "login-remember-me",
                    "remember",
                    checked = isRememberMe
                  ),
                  label(cls := "form-label", `for` := "login-remember-me")(
                    trans.site.rememberMe()
                  )
                )
              else form3.hidden("remember", isRememberMe)
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
              p(cls := "error none")("Invalid code.")
            ),
            turnstile.widget(hidden = true),
            turnstile.submit(trans.site.signIn())(testId("login-submit"))
          ),
          div(cls := "or-separator")(span(trans.site.orSeparator())),
          a(href := addReferrer(routes.Auth.magicLink.url), cls := "button button-empty magic-link")(
            trans.site.logInByEmail()
          )
        )

  def loginTakex3(form: Form[?], isRememberMe: Boolean = true)(using
      TurnstilePublicConfig,
      Option[ValidReferrer]
  )(using Context, Option[AuthCustomUi]) =
    given Translate = oauthClientLanguage
    takex3Login(form, isRememberMe)

  private def takex3Login(form: Form[?], isRememberMe: Boolean)(using
      TurnstilePublicConfig,
      Option[ValidReferrer],
      Context,
      Option[AuthCustomUi],
      Translate
  ) =
    val blankedPasswordError = form.globalError.exists(_.messages.contains("blankedPassword"))
    Page("Sign in")
      .js(esmInit("bits.auth", "login"))
      .css("bits.auth")
      .css("bits.auth-takex3")
      .csp(_.withTurnstile)
      .hrefLangs(Option.empty[LangPath])
      .flag(_.noHeader):
        main(cls := authClasses("auth auth-login box box-pad", summon[Option[AuthCustomUi]]))(
          takex3ConnectionHeader("Login to Lichess"),
          postForm(
            cls := "form3",
            action := addReferrer(routes.Auth.authenticateTakex3.url)
          )(
            div(cls := "one-factor")(
              if blankedPasswordError then
                div(cls := "auth-login__blanked")(
                  p(trans.site.blankedPassword()),
                  a(href := routes.Auth.passwordResetTakex3, cls := "button button-no-upper")(
                    trans.site.passwordReset()
                  )
                )
              else authGlobalError(form),
              form3.group(form("username"), frag("Username or email")): f =>
                div(cls := "text-wrapper")(
                  form3.input(f)(
                    required,
                    autocomplete := "username",
                    placeholder := "Username or email",
                    testId("username")
                  ),
                  clearFieldButton
                ),
              form3.passwordModified(form("password"), frag("Password"))(
                autocomplete := "current-password",
                placeholder := "Password",
                testId("password")
              ),
              div(cls := "password-reset")(
                a(href := routes.Auth.passwordResetTakex3)(frag("Forgot your password?"))
              ),
              form3.hidden("remember", isRememberMe)
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
              p(cls := "error none")("Invalid code.")
            ),
            turnstile.widget(hidden = true),
            turnstile.submit(frag("Sign in"))(testId("login-submit"))
          )
        )

  def signup(form: Form[?], simple: Boolean)(using
      ctx: Context,
      custom: Option[AuthCustomUi]
  )(using TurnstilePublicConfig, Option[ValidReferrer]) =
    given Translate = oauthClientLanguage
    Page(trans.site.signUp.txt())
      .js(esmInit("bits.auth", "signup"))
      .js(fingerprintTag)
      .css("bits.auth")
      .csp(_.withTurnstile)
      .hrefLangs(lila.ui.LangPath(routes.Auth.signup)):
        main(
          cls := List(
            "auth auth-signup box box-pad" -> true,
            "auth-signup--simple" -> simple
          )
        )(
          logoOrConnection,
          authTabs("signup"),
          postForm(
            id := "signup-form",
            cls := "form3",
            action := addReferrer(routes.Auth.signupPost.url),
            autocomplete := "off"
          )(
            authGlobalError(form),
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
                form3.passwordComplexityMeter(trans.site.newPasswordStrength())(
                  cls := f.value.orZero.isEmpty.option("none")
                )
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
            agreement(form("agreement"), form.errors.exists(_.key.startsWith("agreement."))),
            simple.not.option(turnstile.widget(hidden = true)),
            turnstile.submit(trans.site.signUp()),
            simple.option(small(cls := "form-help")(tosLink))
          )
        )

  def signupTakex3(form: Form[?])(using
      Option[ValidReferrer]
  )(using
      Context,
      Option[AuthCustomUi]
  ) =
    given Translate = oauthClientLanguage
    takex3Signup(form)

  private def takex3Signup(form: Form[?])(using
      Option[ValidReferrer],
      Context,
      Option[AuthCustomUi],
      Translate
  ) =
    Page("Create Lichess account")
      .js(esmInit("bits.auth", "signup"))
      .js(fingerprintTag)
      .css("bits.auth")
      .css("bits.auth-takex3")
      .csp(_.withTurnstile)
      .hrefLangs(Option.empty[LangPath])
      .flag(_.noHeader):
        main(
          cls := authClasses("auth auth-signup box box-pad", summon[Option[AuthCustomUi]]) ::: List(
            "auth-signup--takex3" -> true
          )
        )(
          takex3ConnectionHeader("Create Lichess account"),
          postForm(
            id := "signup-form",
            cls := "form3",
            action := addReferrer(routes.Auth.signupPostTakex3.url),
            autocomplete := "off"
          )(
            authGlobalError(form),
            form3.group(form("username"), frag("Username")): f =>
              frag(
                div(cls := "text-wrapper")(
                  form3.input(f)(
                    autofocus,
                    required,
                    autocomplete := "username",
                    placeholder := "Username"
                  ),
                  clearFieldButton
                ),
                p(cls := "error username-exists none")(trans.site.usernameAlreadyUsed())
              ),
            form3.group(form("password"), frag("Password")): f =>
              frag(
                div(cls := "password-wrapper")(
                  form3.input(f, typ = "password")(required)(
                    autocomplete := "new-password",
                    placeholder := "Password"
                  ),
                  form3.passwordRevealButton
                ),
                div(cls := "password-generator")(button("Generate a random password"))
              ),
            form3.group(form("email"), frag("Email")): f =>
              div(cls := "text-wrapper")(
                form3.input(f, typ = "email")(required, placeholder := "Email"),
                clearFieldButton
              ),
            input(id := "signup-fp-input", name := "fp", tpe := "hidden"),
            hiddenAcceptedAgreements,
            turnstile.submit(frag("Create account")),
            small(cls := "form-help")(
              "By registering you accept Lichess' ",
              a(href := routes.Cms.tos)("Terms and conditions")
            )
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

  def passwordReset(form: Form[?], fail: Option[String])(using TurnstilePublicConfig, Context) =
    Page(trans.site.passwordReset.txt())
      .css("bits.auth")
      .csp(_.withTurnstile):
        main(cls := "auth auth-signup box box-pad")(
          boxTop(
            h1(
              fail.isDefined.option(iconTag(Icon.X)(cls := "is-red")),
              trans.site.passwordReset()
            )
          ),
          postForm(cls := "form3", action := routes.Auth.passwordResetApply)(
            fail.map(p(cls := "error")(_)),
            form3.group(form("email"), trans.site.email())(
              form3.input(_, typ = "email")(autofocus, required, autocomplete := "email")
            ),
            turnstile.widget(),
            form3.action(form3.submit(trans.site.emailMeALink()))
          )
        )

  def passwordResetTakex3(form: Form[?], fail: Option[String])(using
      TurnstilePublicConfig,
      Context,
      Option[AuthCustomUi]
  ) =
    given Translate = oauthClientLanguage
    Page("Forgot your password?")
      .css("bits.auth")
      .css("bits.auth-takex3")
      .csp(_.withTurnstile)
      .flag(_.noHeader):
        main(cls := authClasses("auth auth-password-reset box box-pad", summon[Option[AuthCustomUi]]))(
          takex3ConnectionHeader("Forgot your password?"),
          postForm(cls := "form3", action := routes.Auth.passwordResetApplyTakex3)(
            fail.map(p(cls := "error")(_)),
            form3.group(form("email"), frag("Email"))(
              form3.input(_, typ = "email")(
                autofocus,
                required,
                autocomplete := "email",
                placeholder := "Email"
              )
            ),
            turnstile.widget(),
            form3.action(form3.submit(frag("Email me a link"), icon = Option.empty[Icon]))
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

  def passwordResetSentTakex3(email: String)(using Context, Option[AuthCustomUi]) =
    given Translate = oauthClientLanguage
    Page(trans.site.passwordReset.txt())
      .css("bits.auth")
      .css("bits.auth-takex3")
      .flag(_.noHeader):
        main(
          cls := authClasses(
            "auth auth-password-reset auth-password-reset--sent box box-pad",
            summon[Option[AuthCustomUi]]
          )
        )(
          takex3ConnectionHeader("Check your email"),
          p(cls := "auth__takex3-reset-copy")(trans.site.weHaveSentYouAnEmailTo(email)),
          p(cls := "auth__takex3-reset-copy")(trans.site.ifYouDoNotGetTheEmail()),
          ul(cls := "auth__takex3-reset-checklist")(
            li(trans.site.checkAllEmailFolders()),
            li(trans.site.verifyYourAddress(email))
          ),
          a(cls := "button button-empty magic-link", href := routes.Auth.loginTakex3)("Back to login")
        )

  def passwordResetConfirm(token: String, form: Form[?], ok: Option[Boolean] = None)(using
      Context
  )(using me: Me) =
    Page(s"${me.username} - ${trans.site.changePassword.txt()}")
      .css("bits.auth")
      .js(esmInit("bits.auth", "reset")):
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

  def passwordResetConfirmTakex3(
      token: String,
      form: Form[?],
      source: Option[String]
  )(using Context, Option[AuthCustomUi]) =
    given Translate = oauthClientLanguage
    val title = "Reset your Lichess password"
    val confirmAction = source.fold(routes.Auth.passwordResetConfirmApply(token).url)(source =>
      addQueryParam(routes.Auth.passwordResetConfirmApply(token).url, "source", source)
    )
    def confirmForm =
      postForm(cls := "form3", action := confirmAction)(
        form3.hidden(form("token")),
        form3.passwordModified(form("newPasswd1"), frag("New password"))(
          autofocus,
          autocomplete := "new-password",
          placeholder := "New password"
        ),
        form3.passwordModified(form("newPasswd2"), frag("New password again"))(
          autocomplete := "new-password",
          placeholder := "New password again"
        ),
        form3.globalError(form),
        form3.action(
          form3.submit(
            frag("Reset password"),
            icon = Option.empty[Icon]
          )
        )
      )
    Page(title)
      .css("bits.auth")
      .css("bits.auth-takex3")
      .js(esmInit("bits.auth", "reset"))
      .flag(_.noHeader):
        main(
          cls := authClasses(
            "auth auth-password-reset auth-password-reset-confirm box box-pad",
            summon[Option[AuthCustomUi]]
          )
        )(
          takex3ConnectionHeader(title),
          confirmForm
        )

  def passwordResetSuccessTakex3(using Option[AuthCustomUi]) =
    Page("Password reset")
      .css("bits.auth")
      .css("bits.auth-takex3")
      .flag(_.noHeader):
        main(
          cls := authClasses(
            "auth auth-password-reset auth-password-reset--success box box-pad",
            summon[Option[AuthCustomUi]]
          )
        )(
          takex3ConnectionHeader("Password reset"),
          p(cls := "auth__takex3-reset-copy")(
            "Your Lichess password has been updated. Return to Take Take Take to log in with your new password."
          ),
          a(cls := "button auth__takex3-open-app", href := takex3OpenAppLichessLoginUrl)(
            "Return to Take Take Take"
          )
        )

  def magicLink(form: Form[?], fail: Boolean)(using Context, TurnstilePublicConfig, Option[ValidReferrer]) =
    Page("Log in by email")
      .css("bits.auth")
      .csp(_.withTurnstile):
        main(cls := "auth auth-signup box box-pad")(
          boxTop(
            h1(
              fail.option(iconTag(Icon.X)(cls := "is-red")),
              "Log in by email"
            )
          ),
          p("We will send you an email containing a link to log you in."),
          postForm(cls := "form3", action := addReferrer(routes.Auth.magicLinkApply.url))(
            form3.group(form("email"), trans.site.email())(
              form3.input(_, typ = "email")(autofocus, required, autocomplete := "email")
            ),
            turnstile.widget(),
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

  private def authTabs(active: String)(using Context, Translate, Option[ValidReferrer]) =
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

  private def hiddenAcceptedAgreements =
    frag(
      form3.hidden("agreement.assistance", "true"),
      form3.hidden("agreement.nice", "true"),
      form3.hidden("agreement.account", "true")
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
      aria.label := trans.site.clearField.txt(),
      tabindex := -1
    )

  private def oauthClientLanguage(using orig: Translate, custom: Option[AuthCustomUi]): Translate =
    custom.fold(orig): c =>
      orig.translator.to(c.lang)
