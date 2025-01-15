package lila.pref
package ui

import play.api.data.Form

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class TwoFactorUi(helpers: Helpers, ui: AccountUi)(
    domain: lila.core.config.NetDomain
):
  import helpers.{ *, given }
  import trans.tfa as trt

  def setup(form: Form[?])(using Context)(using me: Me) =
    val secret = form("secret").value.orZero
    ui.AccountPage(s"${me.username} - ${trt.twoFactorAuth.txt()}", "twofactor")
      .js(Esm("bits.qrcode")):
        div(cls := "twofactor box box-pad")(
          h1(cls := "box__top")(trt.twoFactorAuth()),
          standardFlash,
          postForm(cls := "form3", action := routes.Account.setupTwoFactor)(
            div(cls := "form-group")(trt.twoFactorHelp()),
            div(cls := "form-group")(
              p(trt.twoFactorAppRecommend()),
              p(
                strong("Android"),
                " : ",
                fragList(
                  List(
                    a(href := "https://2fas.com/")("2FAS"),
                    a(href := "https://authenticatorpro.jmh.me/")("Authenticator Pro"),
                    a(href := "https://getaegis.app/")("Aegis Authenticator")
                  )
                )
              ),
              p(strong("iOS"), " : ", a(href := "https://2fas.com/")("2FAS"))
            ),
            div(cls := "form-group")(trt.scanTheCode()),
            qrcode(
              s"otpauth://totp/${domain}:${me.userId}?secret=${secret}&issuer=${domain}"
            ),
            div(cls := "form-group"):
              trt.ifYouCannotScanEnterX:
                span(cls := "redacted")(secret)
            ,
            div(cls := "form-group explanation")(trt.enterPassword()),
            form3.hidden(form("secret")),
            form3.passwordModified(form("passwd"), trans.site.password())(
              autofocus,
              autocomplete := "current-password"
            ),
            form3.group(form("token"), trt.authenticationCode())(
              form3.input(_)(pattern := "[0-9]{6}", autocomplete := "one-time-code", required)
            ),
            form3.globalError(form),
            div(cls := "form-group")(
              trt.ifYouLoseAccessTwoFactor(a(href := routes.Auth.passwordReset)(trans.site.passwordReset()))
            ),
            form3.action(form3.submit(trt.enableTwoFactor()))
          )
        )

  def disable(form: Form[?])(using Context)(using me: Me) =
    ui.AccountPage(s"${me.username} - ${trt.twoFactorAuth.txt()}", "twofactor"):
      div(cls := "twofactor box box-pad")(
        boxTop(
          h1(
            i(cls := "is-green text", dataIcon := Icon.Checkmark),
            trt.twoFactorEnabled()
          )
        ),
        standardFlash,
        postForm(cls := "form3", action := routes.Account.disableTwoFactor)(
          p(trt.twoFactorToDisable()),
          trt.ifYouLoseAccessTwoFactor(a(href := routes.Auth.passwordReset)(trans.site.passwordReset())),
          form3.passwordModified(form("passwd"), trans.site.password())(autocomplete := "current-password"),
          form3.group(form("token"), trt.authenticationCode())(
            form3.input(_)(pattern := "[0-9]{6}", autocomplete := "one-time-code", required)
          ),
          form3.action(form3.submit(trt.disableTwoFactor()))
        )
      )
