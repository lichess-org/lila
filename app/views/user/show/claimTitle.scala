package views.html.user.show

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User

import controllers.routes

object claimTitle {

  def apply(u: User)(implicit ctx: Context) =
    div(cls := "claim-title")(
      h2(dataIcon := "C", cls := "text")("Congratulations for breaking the 2400 rating threshold!"),
      p(
        "To ensure honest players aren't falsely accused of cheating, we request titled players ",
        "to identify themselves. For instance, ", a(href := routes.User.show("opperwezen"))("opperwezen"), " and ",
        a(href := routes.User.show("DrNykterstein"))("Magnus Carlsen"), " are verified IM, and GM. ",
        "You can confirm your title and decide to remain anonymous. We will not reveal your identity."
      ),
      p(
        "To confirm your title, ", a(href := "https://goo.gl/forms/KymEzEIFpqTO2Jbr1")("please fill in this form"), "."
      ),
      p(
        "If you need help or have any question, feel free to contact us by email at ", contactEmailLink, "."
      ),
      form(action := routes.Pref.verifyTitle, method := "post")(
        button(cls := "button text", dataIcon := "E", name := "v", value := true)("Got it, thanks!"),
        button(cls := "button", name := "v", value := false)("I don't have an official title")
      )
    )
}
