package views.html.ublog

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.ublog.UblogPost
import lila.user.User

object index {

  import views.html.ublog.bits

  def apply(user: User, posts: Paginator[UblogPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog"
    ) {
      main(cls := "box box-pad page ublog-index")(
        h1(s"${user.username} blog"),
        ctx.is(user) option
          div(newPostLink, a(href := routes.Ublog.drafts(user.username))("My drafts")),
        div(cls := "ublog-index__posts")(
          div(cls := "infinite-scroll")(
            posts.currentPageResults map { bits.mini(_) },
            pagerNext(posts, np => s"${routes.Ublog.index(user.username, np).url}")
          )
        )
      )
    }

  def drafts(user: User, posts: Paginator[UblogPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} drafts"
    ) {
      main(cls := "box box-pad page ublog-index")(
        h1(s"${user.username} drafts"),
        newPostLink,
        div(cls := "ublog-index__posts ublog-index__posts--drafts")(
          div(cls := "infinite-scroll")(
            posts.currentPageResults map {
              bits.mini(_, bits.editUrlOf)
            },
            pagerNext(posts, np => s"${routes.Ublog.drafts(user.username, np).url}")
          )
        )
      )
    }

  private def newPostLink(implicit ctx: Context) = ctx.me map { u =>
    a(href := routes.Ublog.form(u.username))("Write a new blog post")
  }
}
