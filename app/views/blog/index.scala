package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.blog.{ FullPost, MiniPost }
import lila.common.paginator.Paginator

import controllers.routes

object index {

  def apply(
      pager: Paginator[io.prismic.Document]
  )(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) = {

    val primaryPost =
      (pager.currentPage == 1).??(pager.currentPageResults.headOption) flatMap FullPost.fromDocument("blog")

    views.html.base.layout(
      title = trans.blog.txt(),
      moreCss = cssTag("blog"),
      csp = bits.csp,
      moreJs = infiniteScrollTag,
      withHrefLangs = lila.i18n.LangList.EnglishJapanese.some
    )(
      main(cls := "page-menu")(
        bits.menu(none),
        div(cls := "blog index page-menu__content page-small box")(
          div(cls := "box__top")(
            h1(trans.officialBlog()),
            (ctx.lang.language != "ja") option a(cls := "atom", href := routes.Blog.atom, dataIcon := "3")
          ),
          primaryPost map { post =>
            frag(
              latestPost(post),
              h2(trans.previousBlogPosts())
            )
          },
          div(cls := "blog-cards list infinitescroll")(
            pager.currentPageResults flatMap MiniPost.fromDocument("blog", "wide") map { post =>
              primaryPost.fold(true)(_.id != post.id) option bits.postCard(post, "paginated".some, h3)
            },
            pagerNext(pager, np => langHrefJP(routes.Blog.index(np)))
          )
        )
      )
    )
  }

  def byYear(year: Int, posts: List[MiniPost])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.blogPostsFromYear.txt(year),
      moreCss = cssTag("blog"),
      csp = bits.csp
    )(
      main(cls := "page-menu")(
        bits.menu(year.some),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(h1(trans.blogPostsFromYear(year))),
          st.section(
            div(cls := "blog-cards")(posts map { bits.postCard(_) })
          )
        )
      )
    )

  private def latestPost(
      post: FullPost
  )(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) = {
    val url = routes.Blog.show(post.id, ref = prismic.maybeRef)
    st.article(
      h2(a(href := url)(post.title)),
      bits.metas(post),
      div(cls := "parts")(
        div(cls := "illustration")(
          a(href := url)(st.img(src := post.image))
        ),
        div(cls := "body")(
          post.doc.getStructuredText(s"${post.coll}.body").map { body =>
            raw(lila.blog.BlogApi.extract(body))
          },
          p(cls := "more")(
            a(
              cls      := "button",
              href     := url,
              dataIcon := "G"
            )(
              trans.continueReadingThis()
            )
          )
        )
      )
    )
  }
}
