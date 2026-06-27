package lila.web
package ui

import play.api.data.Form

import lila.core.misc.AuthCustomUi
import lila.core.net.ValidReferrer
import lila.core.security.TurnstilePublicConfig
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class AuthTakex3Ui(helpers: Helpers):
  import helpers.{ *, given }

  private val openAppLichessLoginUrl =
    "https://taketaketake.com/open-app?target=%2F%3FnewGame%3Dtrue%26lichessLogin%3Dtrue"

  private def addReferrer(url: String)(using referrer: Option[ValidReferrer]): String =
    referrer.fold(url)(ref => addQueryParam(url, "referrer", ref.value))

  private def authClasses(base: String)(using custom: Option[AuthCustomUi]) =
    List(base -> true) ::: custom.map(c => s"auth--${c.cssClass}" -> true).toList

  private def connectionHeader(title: String)(using custom: Option[AuthCustomUi]) = custom match
    case Some(c) => frag(customLogo(c), h1(cls := "auth__page-title")(title))
    case None => h1(cls := "auth__page-title")(title)

  private def customLogo(c: AuthCustomUi) =
    div(cls := "oauth__connection")(
      img(
        src := assetUrl(c.imagePath),
        alt := c.name,
        cls := "oauth__connection__service"
      ),
      div(cls := "oauth__connection__between")(
        connectionDash,
        checkIcon("oauth__connection__check"),
        connectionDash
      ),
      lila.web.ui.bits.logo
    )

  private def connectionDash =
    span(cls := "oauth__connection__dash-pad")(
      raw(
        """<svg class="oauth__connection__dash" width="36" height="2" viewBox="0 0 36 2" fill="none" xmlns="http://www.w3.org/2000/svg"><rect width="3" height="2" fill="currentColor"/><rect x="9" width="6" height="2" fill="currentColor"/><rect x="21" width="6" height="2" fill="currentColor"/><rect x="33" width="3" height="2" fill="currentColor"/></svg>"""
      )
    )

  private val checkSvg =
    """<svg width="17" height="17" viewBox="0 0 17 17" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M6.19758 13.5645L15.3185 4.56042L14.0323 3.27413L6.19758 11.1088L3.04032 7.83461L1.75403 9.12091L6.19758 13.5645Z" fill="white"/></svg>"""

  private def checkIcon(klass: String) =
    span(cls := klass)(raw(checkSvg))

  def login(form: Form[?], isRememberMe: Boolean = true)(using
      TurnstilePublicConfig,
      Option[ValidReferrer]
  )(using Context, Option[AuthCustomUi]) =
    given Translate = oauthClientLanguage
    val blankedPasswordError = form.globalError.exists(_.messages.contains("blankedPassword"))
    Page("Sign in")
      .js(esmInit("bits.auth", "login"))
      .css("bits.auth")
      .css("bits.auth-takex3")
      .csp(_.withTurnstile)
      .hrefLangs(Option.empty[LangPath])
      .flag(_.noHeader):
        main(cls := authClasses("auth auth-login box box-pad"))(
          connectionHeader("Login to Lichess"),
          postForm(
            cls := "form3",
            action := addReferrer(routes.Auth.authenticate.url)
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
      Option[ValidReferrer],
      TurnstilePublicConfig
  )(using
      Context,
      Option[AuthCustomUi]
  ) =
    given Translate = oauthClientLanguage
    Page("Create Lichess account")
      .js(esmInit("bits.auth", "signup"))
      .js(fingerprintTag)
      .css("bits.auth")
      .css("bits.auth-takex3")
      .csp(_.withTurnstile)
      .hrefLangs(Option.empty[LangPath])
      .flag(_.noHeader):
        main(
          cls := authClasses("auth auth-signup box box-pad")
        )(
          connectionHeader("Create Lichess account"),
          postForm(
            id := "signup-form",
            cls := "form3",
            action := addReferrer(routes.Auth.signupPost.url),
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
            simple.not.option(turnstile.widget(hidden = true)),
            turnstile.submit(frag("Create account")),
            small(cls := "form-help")(
              "By registering you accept Lichess' ",
              a(href := routes.Cms.tos)("Terms and conditions")
            )
          )
        )

  def passwordReset(form: Form[?], fail: Option[String])(using
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
        main(cls := authClasses("auth auth-password-reset box box-pad"))(
          connectionHeader("Forgot your password?"),
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

  def passwordResetSent(email: String)(using Context, Option[AuthCustomUi]) =
    given Translate = oauthClientLanguage
    Page(trans.site.passwordReset.txt())
      .css("bits.auth")
      .css("bits.auth-takex3")
      .flag(_.noHeader):
        main(cls := authClasses("auth auth-password-reset auth-password-reset--sent box box-pad"))(
          connectionHeader("Check your email"),
          p(cls := "auth__reset-copy")(trans.site.weHaveSentYouAnEmailTo(email)),
          p(cls := "auth__reset-copy")(trans.site.ifYouDoNotGetTheEmail()),
          ul(cls := "auth__reset-checklist")(
            li(trans.site.checkAllEmailFolders()),
            li(trans.site.verifyYourAddress(email))
          ),
          a(cls := "button button-empty auth__secondary-action", href := routes.Auth.login)(
            "Back to login"
          )
        )

  def passwordResetConfirm(
      token: String,
      form: Form[?]
  )(using Context, Option[AuthCustomUi]) =
    given Translate = oauthClientLanguage
    val title = "Reset your Lichess password"
    def confirmForm =
      postForm(cls := "form3", action := routes.Auth.passwordResetConfirmApplyTakex3(token))(
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
        main(cls := authClasses("auth auth-password-reset auth-password-reset-confirm box box-pad"))(
          connectionHeader(title),
          confirmForm
        )

  def passwordResetSuccess(using Option[AuthCustomUi]) =
    Page("Password reset")
      .css("bits.auth")
      .css("bits.auth-takex3")
      .flag(_.noHeader):
        main(cls := authClasses("auth auth-password-reset auth-password-reset--success box box-pad"))(
          connectionHeader("Password reset"),
          p(cls := "auth__reset-copy")(
            "Your Lichess password has been updated. Return to Take Take Take to log in with your new password."
          ),
          a(cls := "button auth__open-app", href := openAppLichessLoginUrl)(
            "Return to Take Take Take"
          )
        )

  private def hiddenAcceptedAgreements =
    frag(
      form3.hidden("agreement.assistance", "true"),
      form3.hidden("agreement.nice", "true"),
      form3.hidden("agreement.account", "true")
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
