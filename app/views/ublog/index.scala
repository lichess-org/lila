package views.html.ublog

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.ublog.UblogPost
import lila.user.User

object index {

  import views.html.ublog.{ post => postView }

  def apply(user: User, posts: Paginator[UblogPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} blog"
    ) {
      main(cls := "box box-pad page page-small ublog-index")(
        div(cls := "box__top")(
          h1(s"${user.username} Blog"),
          ctx.is(user) option
            div(cls := "box__top__actions")(
              a(href := routes.Ublog.drafts(user.username))("Drafts"),
              newPostLink
            )
        ),
        div(cls := "ublog-index__posts infinite-scroll")(
          posts.currentPageResults map { postView.card(_) },
          pagerNext(posts, np => s"${routes.Ublog.index(user.username, np).url}")
        )
      )
    }

  def drafts(user: User, posts: Paginator[UblogPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${user.username} drafts"
    ) {
      main(cls := "box box-pad page page-small ublog-index")(
        div(cls := "box__top")(
          h1(s"${user.username} drafts"),
          div(cls := "box__top__actions")(
            a(href := routes.Ublog.index(user.username))("Published"),
            newPostLink
          )
        ),
        div(cls := "ublog-index__posts ublog-index__posts--drafts infinite-scroll")(
          posts.currentPageResults map { postView.card(_, postView.editUrlOf) },
          pagerNext(posts, np => s"${routes.Ublog.drafts(user.username, np).url}")
        )
      )
    }

  private def newPostLink(implicit ctx: Context) = ctx.me map { u =>
    a(
      href := routes.Ublog.form(u.username),
      cls := "button button-green",
      dataIcon := "",
      title := "Write a new blog post"
    )
  }
}
