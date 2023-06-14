package views.html
package forum

import controllers.routes

import lila.api.WebContext
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.forum.ForumPost
import lila.security.{ Granter, Permission }

object bits:

  def searchForm(search: String = "")(using WebContext) =
    div(cls := "box__top__actions")(
      form(cls := "search", action := routes.ForumPost.search())(
        input(
          name         := "text",
          value        := search,
          placeholder  := trans.search.search.txt(),
          enterkeyhint := "search"
        )
      )
    )

  def authorLink(post: ForumPost, cssClass: Option[String] = None, withOnline: Boolean = true)(using
      ctx: WebContext
  ): Frag =
    if (!(ctx.me so Granter(Permission.ModerateForum)) && post.erased) span(cls := "author")("<erased>")
    else
      userIdLink(post.userId, cssClass = cssClass, withOnline = withOnline, modIcon = ~post.modIcon)

  private[forum] val dataTopic = attr("data-topic")
  private[forum] val dataUnsub = attr("data-unsub")
