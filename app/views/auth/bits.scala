package views.html
package auth

import play.api.data.{ Form, Field }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object bits {

  def formFields(username: Field, password: Field, emailOption: Option[Field], register: Boolean)(implicit ctx: Context) = frag(
    form3.group(username, if (register) trans.username.frag() else trans.usernameOrEmail.frag()) { f =>
      frag(
        form3.input(f)(autofocus := true),
        p(cls := "error exists none")(trans.usernameAlreadyUsed.frag())
      )
    },
    form3.password(password, trans.password.frag()),
    emailOption.map { email =>
      form3.group(email, trans.email.frag())(form3.input(_, typ = "email"))
    }
  )

  def passwordReset(form: Form[_], captcha: lila.common.Captcha, ok: Option[Boolean] = None)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.passwordReset.txt(),
      responsive = true,
      moreCss = responsiveCssTag("auth")
    ) {
        main(cls := "auth auth-signup box box-pad")(
          h1(
            ok.map { r =>
              span(cls := (if (r) "is-green" else "is-red"), dataIcon := (if (r) "E" else "L"))
            },
            trans.passwordReset.frag()
          ),
          st.form(
            cls := "form3",
            action := routes.Auth.passwordResetApply,
            method := "post"
          )(
              form3.group(form("email"), trans.email.frag())(form3.input(_, typ = "email", autofocus := true)),
              views.html.base.captcha(form, captcha),
              form3.action(form3.submit(trans.emailMeALink.frag()))
            )
        )
      }

  def passwordResetSent(email: String)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.passwordReset.txt(),
      responsive = true
    ) {
        main(cls := "page-small box box-pad")(
          h1(cls := "is-green text", dataIcon := "E")(trans.checkYourEmail.frag()),
          p(trans.weHaveSentYouAnEmailTo.frag(email)),
          p(trans.ifYouDoNotSeeTheEmailCheckOtherPlaces.frag())
        )
      }

  def passwordResetConfirm(u: User, token: String, form: Form[_], ok: Option[Boolean] = None)(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${u.username} - ${trans.changePassword.txt()}",
      responsive = true,
      moreCss = responsiveCssTag("form3")
    ) {
        main(cls := "page-small box box-pad")(
          (ok match {
            case Some(true) => h1(cls := "is-green text", dataIcon := "E")
            case Some(false) => h1(cls := "is-red text", dataIcon := "L")
            case _ => h1
          })(
            userLink(u, withOnline = false),
            " - ",
            trans.changePassword.frag()
          ),
          st.form(cls := "form3", action := routes.Auth.passwordResetConfirmApply(token), method := "POST")(
            form3.hidden(form("token")),
            form3.passwordModified(form("newPasswd1"), trans.newPassword.frag())(autofocus := true),
            form3.password(form("newPasswd2"), trans.newPasswordAgain.frag()),
            form3.globalError(form),
            form3.action(form3.submit(trans.changePassword.frag()))
          )
        )
      }

  def checkYourEmailBanner(userEmail: lila.security.EmailConfirm.UserEmail) = frag(
    styleTag("""
body { margin-top: 45px; }
#email_confirm {
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
  z-index: 100;
}
#email_confirm a {
  color: #fff!important;
  text-decoration: underline;
  margin-left: 1em;
}
"""),
    div(id := "email_confirm")(
      s"Almost there, ${userEmail.username}! Now check your email (${userEmail.email.conceal}) for signup confirmation.",
      a(href := routes.Auth.checkYourEmail)("Click here for help")
    )
  )

  def tor()(implicit ctx: Context) =
    views.html.base.layout(
      responsive = true,
      title = "Tor exit node"
    ) {
      main(cls := "page-small box box-pad")(
        h1(cls := "text", dataIcon := "2")("Ooops"),
        p("Sorry, you can't signup to lichess through TOR!"),
        p("As an Anonymous user, you can play, train, and use all lichess features.")
      )
    }
}
