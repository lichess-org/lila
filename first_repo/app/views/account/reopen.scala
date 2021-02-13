package views.html
package account

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object reopen {

  def form(form: lila.security.RecaptchaForm[_], error: Option[String] = None)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = "Reopen your account",
      moreCss = cssTag("auth"),
      moreJs = views.html.base.recaptcha.script(form),
      csp = defaultCsp.withRecaptcha.some
    ) {
      main(cls := "page-small box box-pad")(
        h1("Reopen your account"),
        p(
          "If you closed your account, but have since changed your mind, you get one chance of getting your account back."
        ),
        p(strong("This will only work once.")),
        p("If you close your account a second time, there will be no way of recovering it."),
        hr,
        postForm(id := form.formId, cls := "form3", action := routes.Account.reopenApply)(
          error.map { err =>
            p(cls := "error")(strong(err))
          },
          form3.group(form("username"), trans.username())(form3.input(_)(autofocus)),
          form3
            .group(form("email"), trans.email(), help = frag("Email address associated to the account").some)(
              form3.input(_, typ = "email")
            ),
          form3.action(views.html.base.recaptcha.button(form)(form3.submit(trans.emailMeALink())))
        )
      )
    }

  def sent(implicit ctx: Context) =
    views.html.base.layout(
      title = "Reopen your account"
    ) {
      main(cls := "page-small box box-pad")(
        h1(cls := "is-green text", dataIcon := "E")(trans.checkYourEmail()),
        p("We've sent you an email with a link."),
        p(trans.ifYouDoNotSeeTheEmailCheckOtherPlaces())
      )
    }
}
