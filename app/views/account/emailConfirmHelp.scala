package views.html
package account

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.security.EmailConfirm.Help._

import controllers.routes

object emailConfirmHelp {

  private val title = "Help with email confirmation"

  def apply(form: Form[_], status: Option[Status])(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("email-confirm")
    )(
      frag(
        main(cls := "page-small box box-pad email-confirm-help")(
          h1(title),
          p("You signed up, but didn't receive your confirmation email?"),
          st.form(cls := "form3", action := routes.Account.emailConfirmHelp, method := "get")(
            form3.split(
              form3.group(
                form("username"),
                trans.username(),
                help = raw("What username did you create?").some
              ) { f =>
                form3.input(f)(pattern := lila.user.User.newUsernameRegex.regex)
              },
              div(cls := "form-group")(
                form3.submit(trans.apply())
              )
            )
          ),
          div(cls := "replies")(
            status map {
              case NoSuchUser(name) =>
                frag(
                  p("We couldn't find any user by this name: ", strong(name), "."),
                  p(
                    "You can use it to ",
                    a(href := routes.Auth.signup)("create a new account"),
                    "."
                  )
                )
              case EmailSent(name, email) =>
                frag(
                  p("We have sent an email to ", email.conceal, "."),
                  p(
                    "It can take some time to arrive.",
                    br,
                    strong("Wait 10 minutes and refresh your email inbox.")
                  ),
                  p("Also check your spam folder, it might end up there. If so, mark it as NOT spam."),
                  p("If everything else fails, then send us this email:"),
                  hr,
                  p(i(s"Hello, please confirm my account: $name")),
                  hr,
                  p(
                    "Copy and paste the above text and send it to ",
                    a(href := s"mailto:$contactEmailInClear?subject=Confirm account $name")(
                      contactEmailInClear
                    )
                  ),
                  p("We will come back to you shortly to help you complete your signup.")
                )
              case Confirmed(name) =>
                frag(
                  p("The user ", strong(name), " is successfully confirmed."),
                  p("You can ", a(href := routes.Auth.login)("login right now as ", name), "."),
                  p("You do not need a confirmation email.")
                )
              case Closed(name) =>
                frag(
                  p("The account ", strong(name), " is closed.")
                )
              case NoEmail(name) =>
                frag(
                  p("The account ", strong(name), " doesn't have an email."),
                  p("Visit the ", a(href := routes.Main.contact)("contact page"), ".")
                )
            }
          )
        )
      )
    )
}
