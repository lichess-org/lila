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
      moreCss = cssTag("ublog"),
      moreJs = ctx.is(user) option jsModule("ublog"),
      title = trans.ublog.xBlog.txt(user.username)
    ) {
      main(cls := "box box-pad page page-small ublog-index")(
        div(cls := "box__top")(
          h1(trans.ublog.xBlog(userLink(user))),
          ctx.is(user) option
            div(cls := "box__top__actions")(
              a(href := routes.Ublog.drafts(user.username))(trans.ublog.drafts()),
              newPostLink
            )
        ),
        standardFlash(),
        if (posts.nbResults > 0)
          div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
            posts.currentPageResults map { postView.card(_) },
            pagerNext(posts, np => s"${routes.Ublog.index(user.username, np).url}")
          )
        else
          div(cls := "ublog-index__posts--empty")(
            trans.ublog.noPostsInThisBlogYet()
          )
      )
    }

  def drafts(user: User, posts: Paginator[UblogPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      title = s"${trans.ublog.drafts()}"
    ) {
      main(cls := "box box-pad page page-small ublog-index")(
        div(cls := "box__top")(
          h1(trans.ublog.drafts()),
          div(cls := "box__top__actions")(
            a(href := routes.Ublog.index(user.username))(trans.ublog.published()),
            newPostLink
          )
        ),
        if (posts.nbResults > 0)
          div(cls := "ublog-index__posts ublog-index__posts--drafts ublog-post-cards infinite-scroll")(
            posts.currentPageResults map { postView.card(_, postView.editUrlOf) },
            pagerNext(posts, np => s"${routes.Ublog.drafts(user.username, np).url}")
          )
        else
          div(cls := "ublog-index__posts--empty")(
            trans.ublog.noDrafts()
          )
      )
    }

  private def newPostLink(implicit ctx: Context) = ctx.me map { u =>
    a(
      href := routes.Ublog.form(u.username),
      cls := "button button-green",
      dataIcon := "",
      title := trans.ublog.newPost.txt()
    )
  }
}
