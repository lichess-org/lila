package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.blog.MiniPost
import lila.common.paginator.Paginator

import controllers.routes

object index {

  def apply(
      pager: Paginator[io.prismic.Document]
  )(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) = {

    val primaryPost = (pager.currentPage == 1).??(pager.currentPageResults.headOption)

    views.html.base.layout(
      title = "Blog",
      moreCss = cssTag("blog"),
      csp = bits.csp,
      moreJs = infiniteScrollTag
    )(
      main(cls := "page-menu")(
        bits.menu(none),
        div(cls := "blog index page-menu__content page-small box")(
          div(cls := "box__top")(
            h1("Lichess Official Blog"),
            a(cls := "atom", href := routes.Blog.atom, dataIcon := "3")
          ),
          primaryPost map { post =>
            frag(
              latestPost(post),
              h2("Previous blog posts")
            )
          },
          div(cls := "blog-cards list infinite-scroll")(
            pager.currentPageResults flatMap MiniPost.fromDocument("blog", "wide") map { post =>
              primaryPost.fold(true)(_.id != post.id) option bits.postCard(post, "paginated".some, h3)
            },
            pagerNext(pager, np => routes.Blog.index(np).url)
          )
        )
      )
    )
  }

  def byYear(year: Int, posts: List[MiniPost])(implicit ctx: Context) =
    views.html.base.layout(
      title = s"Blog posts from $year",
      moreCss = cssTag("blog"),
      csp = bits.csp
    )(
      main(cls := "page-menu")(
        bits.menu(year.some),
        div(cls := "page-menu__content box")(
          div(cls := "box__top")(h1(s"Blog posts from $year")),
          st.section(
            div(cls := "blog-cards")(posts map { bits.postCard(_) })
          )
        )
      )
    )

  private def latestPost(
      doc: io.prismic.Document
  )(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) =
    st.article(
      doc.getText("blog.title").map { title =>
        h2(a(href := routes.Blog.show(doc.id, doc.slug, prismic.maybeRef))(title))
      },
      bits.metas(doc),
      div(cls := "parts")(
        doc.getImage("blog.image", "main").map { img =>
          div(cls := "illustration")(
            a(href := routes.Blog.show(doc.id, doc.slug, ref = prismic.maybeRef))(st.img(src := img.url))
          )
        },
        div(cls := "body")(
          doc.getStructuredText("blog.body").map { body =>
            raw(lila.blog.BlogApi.extract(body))
          },
          p(cls := "more")(
            a(
              cls := "button",
              href := routes.Blog.show(doc.id, doc.slug, ref = prismic.maybeRef),
              dataIcon := "G"
            )(
              " Continue reading this post"
            )
          )
        )
      )
    )
}
