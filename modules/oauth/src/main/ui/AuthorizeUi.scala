package lila.oauth
package ui

import lila.core.LightUser
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.misc.AuthCustomUi

final class AuthorizeUi(helpers: Helpers)(
    lightUserFallback: UserId => LightUser,
    customLogo: AuthCustomUi => Frag
):
  import helpers.{ *, given }

  def apply(prompt: AuthorizationRequest.Prompt, signedClient: Option[OAuthSignedClient])(using
      ctx: Context,
      me: Me
  ) =
    given customUi: Option[AuthCustomUi] = signedClient.flatMap(_.design)
    given Translate = oauthClientLanguage
    val otherUserRequested = prompt.userId.filterNot(me.is(_)).map(lightUserFallback)
    val cssClass = customUi.map(_.cssClass)
    val logo = customUi.map(customLogo) |
      iconTag(Icon.Logo)(alt := "lichess logo", cls := "oauth__logo--font")
    Page(signedClient.fold("Authorization")(c => s"Allow ${c.displayName}"))
      .css("bits.oauth")
      .js(Esm("bits.oauth"))
      .flag(_.noHeader)
      .csp(_.withLegacyUnsafeInlineScripts):
        main(
          cls := s"oauth box box-pad force-ltr${cssClass.so(c => " oauth--" + c)}"
        )(
          div(cls := "oauth__top")(
            logo,
            signedClient match
              case Some(client) => h2("Allow ", span(cls := "oauth__client-name")(client.displayName))
              case None => frag(h2("Allow"), strong(code(prompt.redirectUri.origin)))
          ),
          prompt.redirectUri.insecure.option(flashMessage("warning")("Does not use a secure connection")),
          postForm(
            id := "oauth-authorize",
            action := s"${routes.OAuth.authorizeApply}?${ctx.req.rawQueryString}"
          )(
            p(cls := "oauth__access-text")(
              "to access your ",
              strong(otherUserRequested.fold(me.username)(_.name)),
              " account"
            ),
            if signedClient.isDefined then emptyFrag
            else if prompt.scopes.isEmpty then ul(li("Only public data"))
            else
              ul(cls := "oauth__scopes"):
                prompt.scopes.value.map: scope =>
                  li(cls := List("danger" -> OAuthScope.dangerList.has(scope)))(scope.name())
            ,
            div(cls := "oauth__action")(
              otherUserRequested match
                case Some(otherUser) =>
                  a(cls := "button", href := switchLoginUrl(otherUser.name.some))(
                    "Log in as ",
                    otherUser.name
                  )
                case None =>
                  val danger = prompt.isDanger && signedClient.isEmpty
                  val autoClick = signedClient.isDefined &&
                    me.createdAt.isAfter(nowInstant.minusMinutes(15))
                  submitButton(
                    cls := List(
                      "button button-fat" -> true,
                      "button-green" -> !danger,
                      "button-red ok-cancel-confirm text" -> danger,
                      "disabled" -> signedClient.isEmpty,
                      "auto-click" -> autoClick
                    ),
                    dataIcon := danger.option(Icon.CautionTriangle),
                    signedClient.isEmpty.option(disabled),
                    title := s"The website ${prompt.redirectUri.host | prompt.redirectUri.withoutQuery} will get access to your Lichess account. Continue?"
                  ):
                    signedClient.fold("Authorize"): c =>
                      s"Sign in with ${c.displayName}"
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
          prompt.trusted.not.option:
            p(cls := List("danger" -> prompt.isDanger))("Not owned or operated by lichess.org")
    )

  private def oauthClientLanguage(using orig: Translate, custom: Option[AuthCustomUi]): Translate =
    custom.fold(orig): c =>
      orig.translator.to(c.lang)
