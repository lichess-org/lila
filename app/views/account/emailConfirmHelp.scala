package views.html
package account

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.security.EmailConfirm.Help.Status

object emailConfirmHelp:

  def apply(form: Form[?], status: Option[Status])(using PageContext) =
    views.html.base.layout(
      title = trans.site.emailConfirmHelp.txt(),
      moreCss = cssTag("email-confirm")
    )(
      frag(
        main(cls := "page-small box box-pad email-confirm-help")(
          h1(cls := "box__top")(trans.site.emailConfirmHelp()),
          p(trans.site.emailConfirmNotReceived()),
          st.form(cls := "form3", action := routes.Account.emailConfirmHelp, method := "get")(
            form3.split(
              form3.group(
                form("username"),
                trans.site.username(),
                help = trans.site.whatSignupUsername().some
              ) { f =>
                form3.input(f)(pattern := lila.user.User.newUsernameRegex.regex)
              },
              div(cls := "form-group")(
                form3.submit(trans.site.apply())
              )
            )
          ),
          div(cls := "replies")(
            status.map {
              case Status.NoSuchUser(name) =>
                frag(
                  p(trans.site.usernameNotFound(strong(name))),
                  p(
                    a(href := routes.Auth.signup)(
                      trans.site.usernameCanBeUsedForNewAccount()
                    )
                  )
                )
              case Status.EmailSent(name, email) =>
                frag(
                  p(trans.site.emailSent(email.conceal)),
                  p(
                    trans.site.emailCanTakeSomeTime(),
                    br,
                    strong(trans.site.refreshInboxAfterFiveMinutes())
                  ),
                  p(trans.site.checkSpamFolder()),
                  p(trans.site.emailForSignupHelp()),
                  hr,
                  p(i(s"Hello, please confirm my account: $name")),
                  hr,
                  p(
                    trans.site.copyTextToEmail(
                      a(href := s"mailto:$contactEmailInClear?subject=Confirm account $name")(
                        contactEmailInClear
                      )
                    )
                  ),
                  p(trans.site.waitForSignupHelp())
                )
              case Status.Confirmed(name) =>
                frag(
                  p(trans.site.accountConfirmed(strong(name))),
                  p(trans.site.accountCanLogin(a(href := routes.Auth.login)(name))),
                  p(trans.site.accountConfirmationEmailNotNeeded())
                )
              case Status.Closed(name) =>
                p(trans.site.accountClosed(strong(name)))
              case Status.NoEmail(name) =>
                p(trans.site.accountRegisteredWithoutEmail(strong(name)))
            }
          )
        )
      )
    )
