package lila.oauth
package ui

import lila.core.LightUser
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AuthorizeUi(helpers: Helpers)(lightUserFallback: UserId => LightUser):
  import helpers.{ *, given }

  private def ringsImage = img(
    cls := "oauth__logo",
    alt := "linked rings icon",
    src := assetUrl("images/icons/linked-rings.png")
  )

  private def buttonClass(prompt: AuthorizationRequest.Prompt) =
    s"button${prompt.isDanger.so(" button-red ok-cancel-confirm text")} disabled"

  def apply(prompt: AuthorizationRequest.Prompt, me: User, authorizeUrl: String)(using
      Context
  ) =
    import prompt.{ isDanger, looksLikeLichessMobile as mobile }
    val otherUserRequested = prompt.userId.filterNot(me.is(_)).map(lightUserFallback)
    Page("Authorization")
      .css("bits.oauth")
      .js(Esm("bits.oauth"))
      .csp(_.withUnsafeInlineScripts):
        main(cls := "oauth box box-pad force-ltr")(
          div(cls := "oauth__top")(
            ringsImage,
            h1("Authorize"),
            if mobile
            then h2("Lichess Mobile")
            else strong(code(prompt.redirectUri.clientOrigin))
          ),
          prompt.redirectUri.insecure.option(flashMessage("warning")("Does not use a secure connection")),
          postForm(action := authorizeUrl)(
            p(
              "Grant access to your ",
              strong(otherUserRequested.fold(me.username)(_.name)),
              " account:"
            ),
            if mobile then emptyFrag
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
                  submitButton(
                    cls      := buttonClass(prompt),
                    dataIcon := isDanger.option(Icon.CautionTriangle),
                    disabled := true,
                    id       := "oauth-authorize",
                    title := s"The website ${prompt.redirectUri.host | prompt.redirectUri.withoutQuery} will get access to your Lichess account. Continue?"
                  )("Authorize")
            ),
            footer(prompt, isDanger, otherUserRequested)
          )
        )

  private def switchLoginUrl(to: Option[UserName])(using ctx: Context) =
    addQueryParams(routes.Auth.login.url, Map("switch" -> to.fold("1")(_.value), "referrer" -> ctx.req.uri))

  private def footer(
      prompt: AuthorizationRequest.Prompt,
      isDanger: Boolean,
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
      if prompt.looksLikeLichessMobile
      then p("Not using Lichess Mobile? ", a(href := prompt.cancelUrl)("Cancel"))
      else
        frag(
          (!prompt.trusted).option(
            p(cls := List("danger" -> isDanger))("Not owned or operated by lichess.org")
          ),
          p(cls := "oauth__redirect")("Will redirect to ", prompt.redirectUri.withoutQuery)
        )
    )
