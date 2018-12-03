package views.html

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._
import lila.i18n.{ I18nKeys => trans }

import controllers.routes

object bookmark {

  def toggle(g: lila.game.Game, bookmarked: Boolean)(implicit ctx: Context) = ctx.me map { m =>
    a(cls := List(
      "bookmark" -> true,
      "bookmarked" -> bookmarked,
      "hint--top" -> true
    ), href := routes.Bookmark.toggle(g.id), dataHint := trans.bookmarkThisGame.txt())(
      span(dataIcon := "t", cls := "on is3")(g.showBookmarks),
      span(dataIcon := "s", cls := "off is3")(g.showBookmarks)
    )
  } orElse {
    g.hasBookmarks option span(cls := "bookmark")(
      span(dataIcon := "s", cls := "is3")(g.showBookmarks)
    )
  }
}
