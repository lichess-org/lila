package views.html.oAuth.token

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object create {

  def apply(form: Form[lila.oauth.OAuthForm.token.Data], me: lila.user.User)(implicit ctx: Context) = {

    val title = "New personal API access token"

    views.html.account.layout(title = title, active = "oauth.token")(
      div(cls := "account oauth box box-pad")(
        h1(title),
        postForm(cls := "form3", action := routes.OAuthToken.create())(
          div(cls := "form-group")(
            "Personal access tokens function like ordinary Lichess OAuth access tokens. ",
            "They can be used to authenticate to the API over Basic Authentication."
          ),
          form3.group(
            form("description"),
            raw("Token description"),
            help = raw("For you to remember what this token is for").some
          )(form3.input(_)(autofocus)),
          br,
          br,
          h2("Scopes define the access for personal tokens:"),
          div(cls := "scopes")(
            lila.oauth.OAuthScope.all.map { scope =>
              val disabled = {
                me.noBot && scope == lila.oauth.OAuthScope.Bot.Play && me.count.game > 0
              } || {
                me.isBot && scope == lila.oauth.OAuthScope.Board.Play
              }
              val id = s"oauth-scope-${scope.key.replace(":", "_")}"
              div(
                span(
                  form3.cmnToggle(
                    id,
                    s"${form("scopes").name}[]",
                    value = scope.key,
                    checked = form.value.exists(_.scopes.contains(scope.key)),
                    disabled = disabled
                  )
                ),
                label(`for` := id, st.title := disabled.option("You already have played games!"))(scope.name)
              )
            }
          ),
          form3.actions(
            a(href := routes.OAuthToken.index())("Cancel"),
            form3.submit(trans.apply())
          )
        )
      )
    )
  }
}
