package views.html

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object bookmark:

  def toggle(g: lila.game.Game, bookmarked: Boolean)(using ctx: Context) =
    if ctx.isAuth then
      a(
        cls := List(
          "bookmark"   -> true,
          "bookmarked" -> bookmarked
        ),
        href  := routes.Game.bookmark(g.id),
        title := trans.site.bookmarkThisGame.txt()
      )(
        iconTag(Icon.Star)(cls        := "on is3"),
        iconTag(Icon.StarOutline)(cls := "off is3"),
        span(g.showBookmarks)
      )
    else if g.hasBookmarks then
      span(cls := "bookmark")(
        span(dataIcon := Icon.StarOutline, cls := "is3")(g.showBookmarks)
      )
    else emptyFrag
