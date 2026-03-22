package lila.oauth
package ui

import lila.core.LightUser
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AuthorizeUi(helpers: Helpers)(lightUserFallback: UserId => LightUser):
  import helpers.{ *, given }

  def apply(prompt: AuthorizationRequest.Prompt, signedClient: Option[OAuthSignedClient])(using
      ctx: Context,
      me: Me
  ) =
    val otherUserRequested = prompt.userId.filterNot(me.is(_)).map(lightUserFallback)
    Page("Authorization")
      .css("bits.oauth")
      .js(Esm("bits.oauth"))
      .flag(_.noHeader)
      .csp(_.withLegacyUnsafeInlineScripts):
        main(cls := "oauth box box-pad force-ltr")(
          div(cls := "oauth__top")(
            iconTag(Icon.Logo)(cls := "oauth__logo", alt := "lichess logo"),
            h1("Authorize"),
            signedClient match
              case Some(client) => h2(client.displayName)
              case None => strong(code(prompt.redirectUri.origin))
          ),
          prompt.redirectUri.insecure.option(flashMessage("warning")("Does not use a secure connection")),
          postForm(
            id := "oauth-authorize",
            action := s"${routes.OAuth.authorizeApply}?${ctx.req.rawQueryString}"
          )(
            p(
              "Grant access to your ",
              strong(otherUserRequested.fold(me.username)(_.name)),
              " account:"
            ),
            if signedClient.isDefined then emptyFrag
            else if prompt.scopes.isEmpty then ul(li("Only public data"))
            else
              ul(cls := "oauth__scopes"):
                prompt.scopes.value.map: scope =>
                  li(cls := List("danger" -> OAuthScope.dangerList.has(scope)))(scope.name())
            ,
            form3.actions(
              a(href := prompt.cancelUrl)("Cancel"),
              otherUserRequested match
                case Some(otherUser) =>
                  a(cls := "button", href := switchLoginUrl(otherUser.name.some))(
                    "Log in as ",
                    otherUser.name
                  )
                case None =>
                  val danger = prompt.isDanger && signedClient.isEmpty
                  submitButton(
                    cls := s"button${danger.so(" button-red ok-cancel-confirm text")} disabled",
                    dataIcon := danger.option(Icon.CautionTriangle),
                    disabled := true,
                    title := s"The website ${prompt.redirectUri.host | prompt.redirectUri.withoutQuery} will get access to your Lichess account. Continue?"
                  )("Authorize")
            ),
            footer(prompt, signedClient, otherUserRequested)
          )
        )

  private def switchLoginUrl(to: Option[UserName])(using ctx: Context) =
    addQueryParams(routes.Auth.login.url, Map("switch" -> to.fold("1")(_.value), "referrer" -> ctx.req.uri))

  private def footer(
      prompt: AuthorizationRequest.Prompt,
      signedClient: Option[OAuthSignedClient],
      otherUserRequested: Option[LightUser]
  )(using ctx: Context) =
    div(cls := "oauth__footer")(
      ctx.me.ifTrue(otherUserRequested.isEmpty).map { me =>
        p(
          "Not ",
          me.username,
          "? ",
          a(href := switchLoginUrl(none))(trans.site.signIn())
        )
      },
      signedClient match
        case Some(client) => p(s"Not using ${client.displayName}? ", a(href := prompt.cancelUrl)("Cancel"))
        case None =>
          frag(
            (!prompt.trusted).option(
              p(cls := List("danger" -> prompt.isDanger))("Not owned or operated by lichess.org")
            ),
            p(cls := "oauth__redirect")("Will redirect to ", prompt.redirectUri.withoutQuery)
          )
    )
