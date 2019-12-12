package views.html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bookmark {

  def toggle(g: lila.game.Game, bookmarked: Boolean)(implicit ctx: Context) = ctx.me map { m =>
    a(cls := List(
      "bookmark" -> true,
      "bookmarked" -> bookmarked
    ), href := routes.Bookmark.toggle(g.id), title := trans.bookmarkThisGame.txt())(
      iconTag("t")(cls := "on is3"),
      iconTag("s")(cls := "off is3"),
      span(g.showBookmarks)
    )
  } orElse {
    g.hasBookmarks option span(cls := "bookmark")(
      span(dataIcon := "s", cls := "is3")(g.showBookmarks)
    )
  }
}
