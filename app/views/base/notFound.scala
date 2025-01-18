package views.html
package base

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object notFound {

  def apply()(implicit ctx: Context) =
    layout(
      title = trans.pageNotFound.txt(),
      moreCss = frag(
        cssTag("misc.not-found"),
        vendorCssTag("sliding-puzzles", "hakoirimusume.css")
      ),
      moreJs = frag(
        vendorJsTag("sliding-puzzles", "sliding-puzzles.min.js"),
        jsTag("misc.hakoirimusume")
      )
    ) {
      main(cls := "not-found page-small box box-pad")(
        header(
          h1("404"),
          div(
            strong(trans.pageNotFound.txt()),
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
        )
      )
    }
}
