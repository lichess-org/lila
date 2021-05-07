package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bookmark {

  def toggle(g: lila.game.Game, bookmarked: Boolean)(implicit ctx: Context) =
    if (ctx.isAuth)
      a(
        cls := List(
          "bookmark"   -> true,
          "bookmarked" -> bookmarked
        ),
        href := routes.Game.bookmark(g.id),
        title := trans.bookmarkThisGame.txt()
      )(
        iconTag("t")(cls := "on is3"),
        iconTag("s")(cls := "off is3"),
        span(g.showBookmarks)
      )
    else if (g.hasBookmarks)
      span(cls := "bookmark")(
        span(dataIcon := "s", cls := "is3")(g.showBookmarks)
      )
    else emptyFrag
}
