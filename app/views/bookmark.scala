package views.html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object bookmark {

  def toggle(g: lidraughts.game.Game, bookmarked: Boolean)(implicit ctx: Context) = ctx.me map { m =>
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
