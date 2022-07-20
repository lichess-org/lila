package views.html
package base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object notFound {

  def apply()(implicit ctx: Context) =
    layout(
      title = "Page not found",
      moreCss = frag(
        cssTag("not-found"),
        cssAt("vendor/sliding-puzzles/assets/css/hakoirimusume.css")
      )
    ) {
      main(cls := "not-found page-small box box-pad")(
        header(
          h1("404"),
          div(
            strong("Page not found!"),
            p(
              "Return to ",
              a(href := routes.Lobby.home)("the homepage"),
              span(cls := "or-play")(" or play this mini-game")
            )
          )
        ),
        div(cls := "game-wrap")(
          p(
            cls := "objective"
          )(
            "Your objective is to help the king escape through the bottom hole in the board."
          ),
          div(id := "game"),
          div(cls := "game-help")(
            div(
              span(id := "move-cnt"),
              "moves"
            ),
            button(id := "reset", "RESET")
          ),
          p(cls := "credits")(
            a(
              href   := "https://github.com/WandererXII/sliding-puzzles",
              target := "_blank"
            )("Sliding puzzles")
          )
        ),
        jsAt("vendor/sliding-puzzles/dist/sliding-puzzles.min.js"),
        jsAt("javascripts/hakoirimusume.js")
      )
    }
}
