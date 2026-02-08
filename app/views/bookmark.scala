package views.bookmark

import lila.app.UiEnv.{ *, given }

def toggle(g: Game, bookmarked: Boolean)(using ctx: Context) =
  if ctx.isAuth then
    a(
      cls := List(
        "bookmark" -> true,
        "bookmarked" -> bookmarked
      ),
      href := routes.Game.bookmark(g.id),
      title := trans.site.bookmarkThisGame.txt()
    )(
      iconTag(Icon.Star)(cls := "on is3"),
      iconTag(Icon.StarOutline)(cls := "off is3"),
      span((g.bookmarks > 0).option(g.bookmarks))
    )
  else if g.bookmarks > 0 then
    span(cls := "bookmark")(
      span(dataIcon := Icon.StarOutline, cls := "is3")(g.bookmarks)
    )
  else emptyFrag
