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

  def drafts(user: User, posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = frag(cssTag("ublog")),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = s"${trans.ublog.drafts()}"
    ) {
      main(cls := "box box-pad page page-small ublog-index")(
        div(cls := "box__top")(
          h1(trans.ublog.drafts()),
          div(cls := "box__top__actions")(
            a(href := routes.Ublog.index(user.username))(trans.ublog.published()),
            postView.newPostLink
          )
        ),
        if (posts.nbResults > 0)
          div(cls := "ublog-index__posts ublog-index__posts--drafts ublog-post-cards infinite-scroll")(
            posts.currentPageResults map { postView.card(_, postView.editUrlOfPost) },
            pagerNext(posts, np => routes.Ublog.drafts(user.username, np).url)
          )
        else
          div(cls := "ublog-index__posts--empty")(
            trans.ublog.noDrafts()
          )
      )
    }

  def friends(posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = "Friends blogs"
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, "friends".some),
        main(cls := "page-menu__content box box-pad ublog-index")(
          div(cls := "box__top")(
            h1("Friends blogs")
          ),
          if (posts.nbResults > 0)
            div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
              posts.currentPageResults map { postView.card(_, showAuthor = true) },
              pagerNext(posts, np => routes.Ublog.friends(np).url)
            )
          else
            div(cls := "ublog-index__posts--empty")(
              "Nothing to show. Follow some authors!"
            )
        )
      )
    }

  def community(posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = posts.hasNextPage option infiniteScrollTag,
      title = "Community blogs"
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, "community".some),
        main(cls := "page-menu__content box box-pad ublog-index")(
          div(cls := "box__top")(
            h1("Community blogs")
          ),
          div(cls := "ublog-index__posts ublog-post-cards infinite-scroll")(
            posts.currentPageResults map { postView.card(_, showAuthor = true) },
            pagerNext(posts, np => routes.Ublog.community(np).url)
          )
        )
      )
    }
}
