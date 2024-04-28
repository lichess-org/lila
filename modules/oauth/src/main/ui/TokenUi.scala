package lila.oauth
package ui

import play.api.data.Form

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class TokenUi(helpers: Helpers):
  import helpers.{ *, given }

  import trans.oauthScope as ot

  def index(tokens: List[AccessToken])(using Context) =
    div(cls := "oauth box")(
      boxTop(
        h1(ot.personalAccessTokens()),
        st.form(cls := "box-top__actions", action := routes.OAuthToken.create)(
          submitButton(
            cls      := "button frameless",
            st.title := ot.newAccessToken.txt(),
            dataIcon := Icon.PlusButton
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
          if token.isDangerous then iconTag(Icon.CautionTriangle)(cls := "is-red")
          else iconTag(Icon.Checkmark)(cls                            := "is-green"),
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
                )(trans.site.delete())
              )
            )
          )
      )
    )

  def create(form: Form[OAuthTokenForm.Data], me: User)(using Context) =
    div(cls := "oauth box box-pad")(
      h1(cls := "box__top")(ot.newAccessToken()),
      postForm(cls := "form3", action := routes.OAuthToken.create)(
        div(cls := "form-group")(
          p(ot.tokenGrantsPermission()),
          p(ot.carefullySelect())
        ),
        form3.group(
          form("description"),
          ot.tokenDescription(),
          help = ot.rememberTokenUse().some
        )(form3.input(_)(autofocus)),
        br,
        br,
        h2(ot.whatTheTokenCanDo()),
        div(cls := "scopes")(
          OAuthScope.classified.map: (categ, scopes) =>
            fieldset(
              legend(categ()),
              scopes.map: scope =>
                val disabled = {
                  me.noBot && scope == OAuthScope.Bot.Play && me.count.game > 0
                } || {
                  me.isBot && scope == OAuthScope.Board.Play
                }
                val hidden =
                  scope == OAuthScope.Web.Mod && !(
                    Granter.opt(_.Shusher) || Granter.opt(_.BoostHunter) || Granter.opt(_.CheatHunter)
                  )
                val id = s"oauth-scope-${scope.key.replace(":", "_")}"
                (!hidden).option(
                  div(cls := List("danger" -> OAuthScope.dangerList.has(scope)))(
                    span(
                      form3.cmnToggle(
                        id,
                        s"${form("scopes").name}[]",
                        value = scope.key,
                        checked = form.value.exists(_.scopes.contains(scope.key)),
                        disabled = disabled
                      )
                    ),
                    label(`for` := id, st.title := disabled.option(ot.alreadyHavePlayedGames.txt()))(
                      scope.name(),
                      em(scope.key)
                    )
                  )
                )
            )
        ),
        form3.actions(
          a(href := routes.OAuthToken.index)(trans.site.cancel()),
          form3.submit(trans.site.create())(data("danger-title") := ot.doNotShareIt.txt())
        ),
        br,
        br,
        br,
        div(cls := "force-ltr") {
          val url =
            s"${netBaseUrl}${routes.OAuthToken.create}?scopes[]=challenge:write&scopes[]=puzzle:read&description=Prefilled+token+example"
          frag(
            h2(ot.attentionOfDevelopers()),
            p(
              ot.possibleToPrefill(),
              br,
              ot.forExample(a(href := url)(url)),
              br,
              ot.ticksTheScopes(
                "challenge:create",
                "puzzle:read"
              ),
              br,
              ot.scopesCanBeFound(),
              br,
              ot.givingPrefilledUrls()
            )
          )
        }
      )
    )
