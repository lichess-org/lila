package views.html.oAuth.token

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes
import lila.i18n.I18nKeys.{ oauthScope as ot }

object index:

  def apply(tokens: List[lila.oauth.AccessToken])(using PageContext) =
    views.html.account.layout(title = ot.personalAccessTokens.txt(), active = "oauth.token")(
      div(cls := "account oauth box")(
        boxTop(
          h1(ot.personalAccessTokens()),
          st.form(cls := "box-top__actions", action := routes.OAuthToken.create)(
            submitButton(
              cls      := "button frameless",
              st.title := ot.newAccessToken.txt(),
              dataIcon := licon.PlusButton
            )
          )
        ),
        standardFlash.map(div(cls := "box__pad")(_)),
        p(cls := "box__pad force-ltr")(
          ot.canMakeOauthRequests(
            a(href := s"${routes.Api.index}#section/Introduction/Authentication")(ot.authorizationCodeFlow())
          ),
          br,
          br,
          ot.insteadGenerateToken(
            a(href := routes.OAuthToken.create)(ot.generatePersonalToken())
          ),
          br,
          br,
          ot.guardTokensCarefully(),
          br,
          br,
          ot.apiDocumentationLinks(
            a(href := "https://github.com/lichess-org/api/tree/master/example/oauth-personal-token")(
              ot.personalTokenAppExample()
            ),
            a(href := routes.Api.index)(ot.apiDocumentation())
          )
        ),
        tokens.headOption.filter(_.isBrandNew).map { token =>
          div(cls := "box__pad brand")(
            if token.isDangerous then iconTag(licon.CautionTriangle)(cls := "is-red")
            else iconTag(licon.Checkmark)(cls                            := "is-green"),
            div(
              if token.isDangerous
              then p(strong(ot.doNotShareIt()))
              else p(ot.copyTokenNow()),
              code(token.plain.value)
            )
          )
        },
        table(cls := "slist slist-pad")(
          tokens.map: t =>
            tr(
              td(
                strong(t.description | "Unnamed"),
                br,
                em(t.scopes.value.map(_.name.txt()).mkString(", "))
              ),
              td(cls := "date")(
                t.createdAt.map: created =>
                  frag(ot.created(momentFromNow(created)), br),
                t.usedAt.map: used =>
                  frag(ot.lastUsed(momentFromNow(used)))
              ),
              td(cls := "action")(
                postForm(action := routes.OAuthToken.delete(t.id.value))(
                  submitButton(
                    cls := "button button-red button-empty confirm"
                  )(trans.delete())
                )
              )
            )
        )
      )
    )
