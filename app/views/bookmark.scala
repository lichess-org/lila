package views.html

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object bookmark:

  def toggle(g: lila.game.Game, bookmarked: Boolean)(using ctx: Context) =
    if ctx.isAuth then
      a(
        cls := List(
          "bookmark"   -> true,
          "bookmarked" -> bookmarked
        ),
        href  := routes.Game.bookmark(g.id),
        title := trans.bookmarkThisGame.txt()
      )(
        iconTag(licon.Star)(cls        := "on is3"),
        iconTag(licon.StarOutline)(cls := "off is3"),
        span(g.showBookmarks)
      )
    else if g.hasBookmarks then
      span(cls := "bookmark")(
        span(dataIcon := licon.StarOutline, cls := "is3")(g.showBookmarks)
      )
    else emptyFrag
