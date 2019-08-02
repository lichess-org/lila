package views.html.team

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object admin {

  def changeOwner(t: lidraughts.team.Team, userIds: Iterable[lidraughts.user.User.ID])(implicit ctx: Context) = {

    val title = s"Change owner of Team ${t.name}"

    bits.layout(title = title) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          p("Who do you want to make owner of this team?"),
          br, br,
          postForm(cls := "kick", action := routes.Team.changeOwner(t.id))(
            userIds.toList.sorted.map { userId =>
              button(name := "userId", cls := "button button-empty button-no-upper confirm", value := userId)(
                usernameOrId(userId)
              )
            }
          )
        )
      )
    }
  }

  def kick(t: lidraughts.team.Team, userIds: Iterable[lidraughts.user.User.ID])(implicit ctx: Context) = {

    val title = s"Kick from Team ${t.name}"

    bits.layout(title = title) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          h1(title),
          p("Who do you want to kick out of the team?"),
          br, br,
          postForm(cls := "kick", action := routes.Team.kick(t.id))(
            userIds.toList.sorted.map { userId =>
              button(name := "userId", cls := "button button-empty button-no-upper confirm", value := userId)(
                usernameOrId(userId)
              )
            }
          )
        )
      )
    }
  }
}
