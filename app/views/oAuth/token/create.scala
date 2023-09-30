package views.html.oAuth.token

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.i18n.I18nKeys.{ oauthScope as ot }

object create:

  def apply(form: Form[lila.oauth.OAuthTokenForm.Data], me: lila.user.User)(using PageContext) =

    val title = ot.newAccessToken

    views.html.account.layout(title = title.txt(), active = "oauth.token")(
      div(cls := "account oauth box box-pad")(
        h1(cls := "box__top")(title()),
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
            lila.oauth.OAuthScope.classified.map: (categ, scopes) =>
              fieldset(
                legend(categ()),
                scopes.map: scope =>
                  val disabled = {
                    me.noBot && scope == lila.oauth.OAuthScope.Bot.Play && me.count.game > 0
                  } || {
                    me.isBot && scope == lila.oauth.OAuthScope.Board.Play
                  }
                  val hidden =
                    scope == lila.oauth.OAuthScope.Web.Mod && !(
                      isGranted(_.Shusher) || isGranted(_.BoostHunter) || isGranted(_.CheatHunter)
                    )
                  val id = s"oauth-scope-${scope.key.replace(":", "_")}"
                  !hidden option div(cls := List("danger" -> lila.oauth.OAuthScope.dangerList.has(scope)))(
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
          ),
          form3.actions(
            a(href := routes.OAuthToken.index)(trans.cancel()),
            form3.submit(trans.create())(data("danger-title") := ot.doNotShareIt.txt())
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
    )
