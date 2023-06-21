package views.html
package account

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import play.api.data.Form

import controllers.routes

object twoFactor:

  import trans.tfa.*

  private val qrCode = raw:
    """<div style="width: 276px; height: 276px; padding: 10px; background: white; margin: 2em auto;"><div id="qrcode" style="width: 256px; height: 256px;"></div></div>"""

  def setup(form: Form[?])(using PageContext)(using me: Me) =
    account.layout(
      title = s"${me.username} - ${twoFactorAuth.txt()}",
      active = "twofactor",
      evenMoreJs = frag(
        iifeModule("javascripts/vendor/qrcode.min.js"),
        iifeModule("javascripts/twofactor.form.js")
      )
    ) {
      div(cls := "account twofactor box box-pad")(
        h1(cls := "box__top")(twoFactorAuth()),
        standardFlash,
        postForm(cls := "form3", action := routes.Account.setupTwoFactor)(
          div(cls := "form-group")(twoFactorHelp()),
          div(cls := "form-group")(
            twoFactorApp(
              a(
                href := "https://play.google.com/store/apps/details?id=org.shadowice.flocke.andotp"
              )("Android"),
              a(href := "https://itunes.apple.com/app/google-authenticator/id388497605?mt=8")("iOS")
            )
          ),
          div(cls := "form-group")(scanTheCode()),
          qrCode,
          div(cls := "form-group")(
            ifYouCannotScanEnterX(
              span(style := "background:black;color:black;")(form("secret").value.orZero: String)
            )
          ),
          div(cls := "form-group explanation")(enterPassword()),
          form3.hidden(form("secret")),
          form3.passwordModified(form("passwd"), trans.password())(
            autofocus,
            autocomplete := "current-password"
          ),
          form3.group(form("token"), authenticationCode())(
            form3.input(_)(pattern := "[0-9]{6}", autocomplete := "one-time-code", required)
          ),
          form3.globalError(form),
          div(cls := "form-group")(ifYouLoseAccess()),
          form3.action(form3.submit(enableTwoFactor()))
        )
      )
    }

  def disable(form: Form[?])(using PageContext)(using me: Me) =
    account.layout(
      title = s"${me.username} - ${twoFactorAuth.txt()}",
      active = "twofactor"
    ) {
      div(cls := "account twofactor box box-pad")(
        boxTop(
          h1(
            i(cls := "is-green text", dataIcon := licon.Checkmark),
            twoFactorEnabled()
          )
        ),
        standardFlash,
        postForm(cls := "form3", action := routes.Account.disableTwoFactor)(
          p(twoFactorDisable()),
          form3.passwordModified(form("passwd"), trans.password())(autocomplete := "current-password"),
          form3.group(form("token"), authenticationCode())(
            form3.input(_)(pattern := "[0-9]{6}", autocomplete := "one-time-code", required)
          ),
          form3.action(form3.submit(disableTwoFactor()))
        )
      )
    }
