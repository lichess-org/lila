package views.html.oAuth.token

import play.api.data.Form

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object create {

  def apply(form: Form[_], me: lidraughts.user.User)(implicit ctx: Context) = {

    val title = "New personal API access token"

    views.html.account.layout(title = title, active = "oauth.token")(
      div(cls := "account oauth box box-pad")(
        h1(title),
        postForm(cls := "form3", action := routes.OAuthToken.create)(
          div(cls := "form-group")(
            "Personal access tokens function like ordinary lidraughts OAuth access tokens. ",
            "They can be used to authenticate to the API over Basic Authentication."
          ),
          form3.group(
            form("description"),
            raw("Token description"),
            help = raw("For you to remember what this token is for").some
          )(form3.input(_)),
          br, br,
          h2("Scopes define the access for personal tokens:"),
          div(cls := "scopes")(
            lidraughts.oauth.OAuthScope.all.map { scope =>
              val disabled = me.noBot && scope == lidraughts.oauth.OAuthScope.Bot.Play && me.count.game > 0
              val id = s"oauth-scope-${scope.key.replace(":", "_")}"
              div(
                span(
                  input(st.id := id, cls := "cmn-toggle", tpe := "checkbox", name := s"${form("scopes").name}[]", value := scope.key, disabled option st.disabled),
                  label(`for` := id)
                ),
                label(`for` := id, st.title := disabled.option("You already have played games!"))(scope.name)
              )
            }
          ),
          form3.actions(
            a(href := routes.OAuthToken.index)("Cancel"),
            form3.submit(trans.apply())
          )
        )
      )
    )
  }
}
