package views.html.ublog

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.ublog.{ UblogBlog, UblogPost }
import lila.user.User
import play.api.mvc.Call

object blog {

  import views.html.ublog.{ post => postView }

  def apply(user: User, blog: UblogBlog, posts: Paginator[UblogPost.PreviewPost])(implicit ctx: Context) = {
    val title = trans.ublog.xBlog.txt(user.username)
    views.html.base.layout(
      moreCss = cssTag("ublog"),
      moreJs = frag(
        posts.hasNextPage option infiniteScrollTag,
        ctx.isAuth option jsModule("ublog")
      ),
      title = title,
      atomLinkTag = link(
        href     := routes.Ublog.userAtom(user.username),
        st.title := title
      ).some,
      robots = netConfig.crawlable && blog.listed
    ) {
      main(cls := "page-menu")(
        views.html.blog.bits.menu(none, (if (ctx is user) "mine" else "community").some),
        div(cls := "page-menu__content box box-pad ublog-index")(
          div(cls := "box__top")(
            h1(trans.ublog.xBlog(userLink(user))),
            div(cls := "box__top__actions")(
              if (ctx is user)
                frag(
                  a(href := routes.Ublog.drafts(user.username))(trans.ublog.drafts()),
                  postView.newPostLink
                )
              else
                frag(
                  isGranted(_.ModerateBlog) option tierForm(blog),
                  a(
                    cls      := "atom",
                    st.title := "Atom RSS feed",
                    href     := routes.Ublog.userAtom(user.username),
                    dataIcon := ""
                  )
                )
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
      )
    }
  }

  def urlOfBlog(blog: UblogBlog): Call = urlOfBlog(blog.id)
  def urlOfBlog(blogId: UblogBlog.Id): Call = blogId match {
    case UblogBlog.Id.User(userId) => routes.Ublog.index(usernameOrId(userId))
  }

  private def tierForm(blog: UblogBlog) = postForm(action := routes.Ublog.setTier(blog.id.full)) {
    val form = lila.ublog.UblogForm.tier.fill(blog.tier)
    frag(
      span(dataIcon := "", cls := "text")("Set to:"),
      form3.select(form("tier"), lila.ublog.UblogBlog.Tier.options)
    )
  }
}
