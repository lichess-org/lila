package views.html
package account

import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.security.EmailConfirm.Help.Status

import controllers.routes

object emailConfirmHelp:

  def apply(form: Form[?], status: Option[Status])(using PageContext) =
    views.html.base.layout(
      title = trans.emailConfirmHelp.txt(),
      moreCss = cssTag("email-confirm")
    )(
      frag(
        main(cls := "page-small box box-pad email-confirm-help")(
          h1(cls := "box__top")(trans.emailConfirmHelp()),
          p(trans.emailConfirmNotReceived()),
          st.form(cls := "form3", action := routes.Account.emailConfirmHelp, method := "get")(
            form3.split(
              form3.group(
                form("username"),
                trans.username(),
                help = trans.whatSignupUsername().some
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
              case Status.NoSuchUser(name) =>
                frag(
                  p(trans.usernameNotFound(strong(name))),
                  p(
                    a(href := routes.Auth.signup)(
                      trans.usernameCanBeUsedForNewAccount()
                    )
                  )
                )
              case Status.EmailSent(name, email) =>
                frag(
                  p(trans.emailSent(email.conceal)),
                  p(
                    trans.emailCanTakeSomeTime(),
                    br,
                    strong(trans.refreshInboxAfterFiveMinutes())
                  ),
                  p(trans.checkSpamFolder()),
                  p(trans.emailForSignupHelp()),
                  hr,
                  p(i(s"Hello, please confirm my account: $name")),
                  hr,
                  p(
                    trans.copyTextToEmail(
                      a(href := s"mailto:$contactEmailInClear?subject=Confirm account $name")(
                        contactEmailInClear
                      )
                    )
                  ),
                  p(trans.waitForSignupHelp())
                )
              case Status.Confirmed(name) =>
                frag(
                  p(trans.accountConfirmed(strong(name))),
                  p(trans.accountCanLogin(a(href := routes.Auth.login)(name))),
                  p(trans.accountConfirmationEmailNotNeeded())
                )
              case Status.Closed(name) =>
                p(trans.accountClosed(strong(name)))
              case Status.NoEmail(name) =>
                p(trans.accountRegisteredWithoutEmail(strong(name)))
            }
          )
        )
      )
    )
