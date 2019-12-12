package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object impersonate {

  def apply(user: lila.user.User)(implicit ctx: Context) =
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
