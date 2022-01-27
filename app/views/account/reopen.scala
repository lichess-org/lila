package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object reopen {

  def form(form: lila.security.HcaptchaForm[_], error: Option[String] = None)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = trans.reopenYourAccount.txt(),
      moreCss = cssTag("auth"),
      moreJs = views.html.base.hcaptcha.script(form),
      csp = defaultCsp.withHcaptcha.some
    ) {
      main(cls := "page-small box box-pad")(
        h1(trans.reopenYourAccount()),
        p(trans.closedAccountChangedMind()),
        p(strong(trans.onlyWorksOnce())),
        p(trans.cantDoThisTwice()),
        hr,
        postForm(cls := "form3", action := routes.Account.reopenApply)(
          error.map { err =>
            p(cls := "error")(strong(err))
          },
          form3.group(form("username"), trans.username())(form3.input(_)(autofocus)),
          form3
            .group(form("email"), trans.email(), help = trans.emailAssociatedToaccount().some)(
              form3.input(_, typ = "email")
            ),
          views.html.base.hcaptcha.tag(form),
          form3.action(form3.submit(trans.emailMeALink()))
        )
      )
    }

  def sent(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.reopenYourAccount.txt()
    ) {
      main(cls := "page-small box box-pad")(
        h1(cls := "is-green text", dataIcon := "")(trans.checkYourEmail()),
        p(trans.sentEmailWithLink()),
        p(trans.ifYouDoNotSeeTheEmailCheckOtherPlaces())
      )
    }
}
