package views.html
package auth

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def passwordReset(form: Form[_], captcha: lidraughts.common.Captcha, ok: Option[Boolean] = None)(implicit ctx: Context) =
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
}
