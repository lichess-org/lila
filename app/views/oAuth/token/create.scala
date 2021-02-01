package views.html.oAuth.token

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

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
          ),
          br,
          div {
            val url =
              s"${netBaseUrl}${routes.OAuthToken.create()}?scopes[]=challenge:write&scopes[]=puzzle:read&description=Prefilled+token+example"
            frag(
              h2("Note for the attention of developers only:"),
              p(
                "It is possible to pre-fill this form by tweaking the query parameters of the URL.",
                br,
                "For example: ",
                a(href := url)(url),
                br,
                "ticks the challenge:create and puzzle:read permissions, and sets the token description.",
                br,
                "The permission codes can be found in the HTML code of the form.",
                br,
                "Giving these pre-filled URLs to your users will help them get the right token permissions."
              )
            )
          }
        )
      )
    )
  }
}
