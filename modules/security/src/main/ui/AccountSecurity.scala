package lila.security
package ui

import play.api.data.Form

import lila.core.id.SessionId
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AccountSecurity(helpers: Helpers)(
    contactEmail: EmailAddress,
    AccountPage: (String, String) => Context ?=> Page
):
  import helpers.{ *, given }

  def apply(
      u: User,
      sessions: List[lila.security.LocatedSession],
      curSessionId: Option[SessionId],
      clients: List[lila.oauth.AccessTokenApi.Client],
      personalAccessTokens: Int
  )(using Context) =
    AccountPage(s"${u.username} - ${trans.site.security.txt()}", "security"):
      div(cls := "security")(
        div(cls := "box")(
          h1(cls := "box__top")(trans.site.security()),
          standardFlash.map(div(cls := "box__pad")(_)),
          div(cls := "box__pad")(
            p(
              "This is a list of devices and applications that are logged into your account. If you notice any suspicious activity, make sure to ",
              a(href := routes.Account.email)("check your recovery email address"),
              " and ",
              a(href := routes.Account.passwd)("change your password"),
              "."
            ),
            (sessions.sizeIs > 1).option(
              div(
                "You can also ",
                postForm(cls := "revoke-all", action := routes.Account.signout("all"))(
                  submitButton(cls := "button button-empty button-red yes-no-confirm")(
                    trans.site.revokeAllSessions()
                  )
                ),
                "."
              )
            )
          ),
          table(sessions, curSessionId, clients, personalAccessTokens)
        )
      )

  private def table(
      sessions: List[lila.security.LocatedSession],
      curSessionId: Option[SessionId],
      clients: List[lila.oauth.AccessTokenApi.Client],
      personalAccessTokens: Int
  )(using Translate) =
    st.table(cls := "slist slist-pad")(
      sessions.map { s =>
        tr(
          td(cls := "icon")(
            span(
              cls := curSessionId.map { cur => s"is-${if cur == s.session.id then "gold" else "green"}" },
              dataIcon := (if s.session.isMobile then Icon.PhoneMobile else Icon.ScreenDesktop)
            )
          ),
          td(cls := "info")(
            span(cls := "ip")(s.session.ip.value),
            " ",
            span(cls := "location")(s.location.map(_.toString)),
            p(cls := "ua")(s.session.ua),
            s.session.date.map { date =>
              p(cls := "date")(
                momentFromNow(date),
                curSessionId.has(s.session.id).option(span(cls := "current")("[CURRENT]"))
              )
            }
          ),
          curSessionId.map { cur =>
            td(
              (s.session != cur).option(
                postForm(action := routes.Account.signout(s.session.id.value))(
                  submitButton(
                    cls := "button button-red",
                    title := trans.site.logOut.txt(),
                    dataIcon := Icon.X
                  )
                )
              )
            )
          }
        )
      },
      clients.map { client =>
        tr(
          td(cls := "icon")(span(cls := "is-green", dataIcon := Icon.ThreeCheckStack)),
          td(cls := "info")(
            strong(client.origin),
            p(cls := "ua")(
              if client.scopes.nonEmpty then
                frag(
                  "Third party application with permissions: ",
                  client.scopes.map(_.name.txt()).mkString(", ")
                )
              else frag("Third party application using only public data.")
            ),
            client.usedAt.map { usedAt =>
              p(cls := "date")(
                "Last used ",
                momentFromNow(usedAt)
              )
            }
          ),
          td(
            postForm(action := routes.OAuth.revokeClient)(
              form3.hidden("origin", client.origin),
              submitButton(cls := "button button-red", title := "Revoke", dataIcon := Icon.X)
            )
          )
        )
      },
      (personalAccessTokens > 0).option(
        tr(
          td(cls := "icon")(span(cls := "is-green", dataIcon := Icon.Tools)),
          td(cls := "info")(
            strong("Personal access tokens"),
            " can be used to access your account. Revoke any that you do not recognize."
          ),
          td(
            a(
              href := routes.OAuthToken.index,
              cls := "button",
              title := trans.oauthScope.apiAccessTokens.txt(),
              dataIcon := Icon.Gear
            )
          )
        )
      )
    )

  import lila.security.EmailConfirm.Help.Status
  def emailConfirmHelp(form: Form[?], status: Option[Status])(using Context) =
    Page(trans.site.emailConfirmHelp.txt()).css("email-confirm"):
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
                form3.input(f)(pattern := lila.user.nameRules.newUsernameRegex.regex)
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
                      a(href := s"mailto:${contactEmail.value}?subject=Confirm account $name")(
                        contactEmail.value
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
