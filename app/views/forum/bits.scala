package views.html
package forum

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.forum.Post
import lila.security.{ Granter, Permission }

object bits {

  def searchForm(search: String = "")(implicit ctx: Context) =
    div(cls := "box__top__actions")(
      form(cls     := "search", action := routes.ForumPost.search())(
        input(name := "text", value    := search, placeholder := trans.search.search.txt())
      )
    )

  def authorLink(post: Post, cssClass: Option[String] = None, withOnline: Boolean = true)(implicit
      ctx: Context
  ): Frag =
    if (!(ctx.me ?? Granter(Permission.ModerateForum)) && post.erased) span(cls := "author")("<erased>")
    else
      userIdLink(post.userId, cssClass = cssClass, withOnline = withOnline, modIcon = ~post.modIcon)(ctx.lang)

  private[forum] val dataTopic = attr("data-topic")
  private[forum] val dataUnsub = attr("data-unsub")
}
