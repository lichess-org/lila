package views.auth

import play.api.data.{ Field, Form }

import lila.app.templating.Environment.{ *, given }

import lila.core.security.HcaptchaForm

object bits:

  def formFields(username: Field, password: Field, email: Option[Field], register: Boolean)(using
      PageContext
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
      email.map { email =>
        form3.group(email, trans.site.email(), help = trans.site.signupEmailHint().some)(
          form3.input(_, typ = "email")(required)
        )
      }
    )

  def passwordReset(form: HcaptchaForm[?], fail: Boolean)(using PageContext) =
    views.base.layout(
      title = trans.site.passwordReset.txt(),
      moreCss = cssTag("auth"),
      modules = hcaptchaScript(form),
      csp = defaultCsp.withHcaptcha.some
    ):
      main(cls := "auth auth-signup box box-pad")(
        boxTop(
          h1(
            fail.option(span(cls := "is-red", dataIcon := Icon.X)),
            trans.site.passwordReset()
          )
        ),
        postForm(cls := "form3", action := routes.Auth.passwordResetApply)(
          form3.group(form("email"), trans.site.email())(
            form3.input(_, typ = "email")(autofocus, required, autocomplete := "email")
          ),
          lila.ui.bits.hcaptcha(form),
          form3.action(form3.submit(trans.site.emailMeALink()))
        )
      )

  def passwordResetSent(email: String)(using PageContext) =
    views.base.layout(
      title = trans.site.passwordReset.txt()
    ):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)(trans.site.checkYourEmail())),
        p(trans.site.weHaveSentYouAnEmailTo(email)),
        p(trans.site.ifYouDoNotSeeTheEmailCheckOtherPlaces())
      )

  def passwordResetConfirm(token: String, form: Form[?], ok: Option[Boolean] = None)(using PageContext)(using
      me: Me
  ) =
    views.base.layout(
      title = s"${me.username} - ${trans.site.changePassword.txt()}",
      moreCss = cssTag("form3"),
      modules = jsModuleInit("bits.passwordComplexity")
    ):
      main(cls := "page-small box box-pad")(
        boxTop(
          (ok match
            case Some(true)  => h1(cls := "is-green text", dataIcon := Icon.Checkmark)
            case Some(false) => h1(cls := "is-red text", dataIcon := Icon.X)
            case _           => h1
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

  def magicLink(form: HcaptchaForm[?], fail: Boolean)(using PageContext) =
    views.base.layout(
      title = "Log in by email",
      moreCss = cssTag("auth"),
      modules = hcaptchaScript(form),
      csp = defaultCsp.withHcaptcha.some
    ):
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

  def magicLinkSent(using PageContext) =
    views.base.layout(
      title = "Log in by email"
    ):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := Icon.Checkmark)(trans.site.checkYourEmail())),
        p("We've sent you an email with a link."),
        p(trans.site.ifYouDoNotSeeTheEmailCheckOtherPlaces())
      )

  def tokenLoginConfirmation(user: User, token: String, referrer: Option[String])(using PageContext) =
    views.base.layout(
      title = s"Log in as ${user.username}",
      moreCss = cssTag("form3")
    ):
      main(cls := "page-small box box-pad")(
        boxTop(h1("Log in as ", userLink(user))),
        postForm(action := routes.Auth.loginWithTokenPost(token, referrer))(
          form3.actions(
            a(href := routes.Lobby.home)(trans.site.cancel()),
            submitButton(cls := "button")(s"${user.username} is my Lichess username, log me in")
          )
        )
      )

  def checkYourEmailBanner(userEmail: lila.security.EmailConfirm.UserEmail) =
    frag(
      styleTag("""
body { margin-top: 45px; }
#email-confirm {
  height: 40px;
  background: #3893E8;
  color: #fff!important;
  font-size: 1.3em;
  display: flex;
  flex-flow: row nowrap;
  justify-content: center;
  align-items: center;
  border-bottom: 1px solid #666;
  box-shadow: 0 5px 6px rgba(0, 0, 0, 0.3);
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 107;
}
#email-confirm a {
  color: #fff!important;
  text-decoration: underline;
  margin-left: 1em;
}
"""),
      div(id := "email-confirm")(
        s"Almost there, ${userEmail.username}! Now check your email (${userEmail.email.conceal}) for signup confirmation.",
        a(href := routes.Auth.checkYourEmail)("Click here for help")
      )
    )

  def tor()(using PageContext) =
    views.base.layout(title = "Tor exit node"):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "text", dataIcon := "2")("Ooops")),
        p("Sorry, you can't signup to Lichess through Tor!"),
        p("You can play, train and use almost all Lichess features as an anonymous user.")
      )

  def logout()(using PageContext) =
    views.base.layout(title = trans.site.logOut.txt()):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.logOut()),
        form(action := routes.Auth.logout, method := "post")(
          button(cls := "button button-red", tpe := "submit")(trans.site.logOut.txt())
        )
      )
