package views.html.mod

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object impersonate {

  def apply(user: lidraughts.user.User)(implicit ctx: Context) =
    div(id := "impersonate")(
      div(cls := "meat")(
        "You are impersonating ",
        userLink(user, withOnline = false)
      ),
      div(cls := "actions")(
        postForm(action := routes.Mod.impersonate("-"))(
          submitButton(cls := "button button-empty")("Quit")
        )
      )
    )
}
