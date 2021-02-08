package views.html.auth

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object checkYourEmail {

  def apply(
      userEmail: Option[lila.security.EmailConfirm.UserEmail],
      form: Option[Form[_]] = None
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "Check your email",
      moreCss = cssTag("email-confirm")
    ) {
      main(
        cls := s"page-small box box-pad email-confirm ${if (form.exists(_.hasErrors)) "error" else "anim"}"
      )(
        h1(cls := "is-green text", dataIcon := "E")(trans.checkYourEmail()),
        p(trans.weHaveSentYouAnEmailClickTheLink()),
        h2("Not receiving it?"),
        ol(
          li(h3(trans.ifYouDoNotSeeTheEmailCheckOtherPlaces())),
          userEmail.map(_.email).map { email =>
            li(
              h3("Make sure your email address is correct:"),
              br,
              br,
              postForm(action := routes.Auth.fixEmail)(
                input(
                  id := "new-email",
                  tpe := "email",
                  required,
                  name := "email",
                  value := form.flatMap(_("email").value).getOrElse(email.value),
                  pattern := s"^((?!^${email.value}$$).)*$$"
                ),
                embedJsUnsafeLoadThen("""
var email = document.getElementById("new-email");
var currentError = "This is already your current email.";
email.setCustomValidity(currentError);
email.addEventListener("input", function() {
email.setCustomValidity(email.validity.patternMismatch ? currentError : "");
      });"""),
                submitButton(cls := "button")("Change it"),
                form.map { f =>
                  errMsg(f("email"))
                }
              )
            )
          },
          li(
            h3("Wait up to 5 minutes."),
            br,
            "Depending on your email provider, it can take a while to arrive."
          ),
          li(
            h3("Still not getting it?"),
            br,
            "Did you make sure your email address is correct?",
            br,
            "Did you wait 5 minutes?",
            br,
            "If so, ",
            a(href := routes.Account.emailConfirmHelp)("proceed to this page to solve the issue"),
            "."
          )
        )
      )
    }
}
