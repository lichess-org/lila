package views.html

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object bookmark:

  def toggle(g: lila.game.Game, bookmarked: Boolean)(implicit ctx: Context) =
    if (ctx.isAuth)
      a(
        cls := List(
          "bookmark"   -> true,
          "bookmarked" -> bookmarked
        ),
        href  := routes.Game.bookmark(g.id),
        title := trans.bookmarkThisGame.txt()
      )(
        iconTag("")(cls := "on is3"),
        iconTag("")(cls := "off is3"),
        span(g.showBookmarks)
      )
    else if (g.hasBookmarks)
      span(cls := "bookmark")(
        span(dataIcon := "", cls := "is3")(g.showBookmarks)
      )
    else emptyFrag
