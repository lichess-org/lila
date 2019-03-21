package views.html
package auth

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object bits {

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
              form3.group(form("email"), trans.email.frag())(form3.input(_, typ = "email")),
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
}
