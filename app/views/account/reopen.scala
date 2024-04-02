package views.html
package account

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object reopen:

  def form(form: lila.security.HcaptchaForm[?], error: Option[String] = None)(using
      ctx: PageContext
  ) =
    views.html.base.layout(
      title = trans.site.reopenYourAccount.txt(),
      moreCss = cssTag("auth"),
      moreJs = views.html.base.hcaptcha.script(form),
      csp = defaultCsp.withHcaptcha.some
    ):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.reopenYourAccount()),
        p(trans.site.closedAccountChangedMind()),
        p(strong(trans.site.onlyWorksOnce())),
        p(trans.site.cantDoThisTwice()),
        hr,
        postForm(cls := "form3", action := routes.Account.reopenApply)(
          error.map: err =>
            p(cls := "error")(strong(err)),
          form3.group(form("username"), trans.site.username())(form3.input(_)(autofocus)),
          form3
            .group(form("email"), trans.site.email(), help = trans.site.emailAssociatedToaccount().some)(
              form3.input(_, typ = "email")
            ),
          views.html.base.hcaptcha.tag(form),
          form3.action(form3.submit(trans.site.emailMeALink()))
        )
      )

  def sent(using PageContext) =
    views.html.base.layout(
      title = trans.site.reopenYourAccount.txt()
    ):
      main(cls := "page-small box box-pad")(
        boxTop(h1(cls := "is-green text", dataIcon := licon.Checkmark)(trans.site.checkYourEmail())),
        p(trans.site.sentEmailWithLink()),
        p(trans.site.ifYouDoNotSeeTheEmailCheckOtherPlaces())
      )
