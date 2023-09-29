package views.html.blog

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.blog.{ BlogPost, MiniPost }
import lila.common.paginator.Paginator

import controllers.routes

object index:

  def apply(
      pager: Paginator[BlogPost]
  )(using ctx: PageContext, prismic: lila.blog.BlogApi.Context) =

    val primaryPost = (pager.currentPage == 1).so(pager.currentPageResults.headOption)

    views.html.base.layout(
      title = "Blog",
      moreCss = cssTag("blog"),
      csp = bits.csp,
      moreJs = infiniteScrollTag
    )(
      main(cls := "page-menu")(
        bits.menu(none, "lichess".some),
        div(cls := "blog index page-menu__content page-small box force-ltr")(
          boxTop(
            h1("Lichess Official Blog"),
            a(cls := "atom", st.title := "Atom RSS feed", href := routes.Blog.atom, dataIcon := licon.RssFeed)
          ),
          primaryPost map { post =>
            frag(
              latestPost(post),
              h2("Previous blog posts")
            )
          },
          div(cls := "blog-cards box__pad list infinite-scroll")(
            pager.currentPageResults flatMap MiniPost.apply map { post =>
              primaryPost.forall(_.id != post.id) option bits.postCard(post, "paginated".some, h3)
            },
            pagerNext(pager, np => routes.Blog.index(np).url)
          )
        )
      )
    )

  def byYear(year: Int, posts: List[MiniPost])(using PageContext) =
    views.html.base.layout(
      title = s"Lichess blog posts from $year",
      moreCss = cssTag("blog"),
      csp = bits.csp
    )(
      main(cls := "page-menu")(
        bits.menu(year.some, none),
        div(cls := "page-menu__content box box-pad force-ltr")(
          boxTop(h1(s"Lichess blog posts from $year")),
          st.section(
            div(cls := "blog-cards")(posts map { bits.postCard(_) })
          )
        )
      )
    )

  private def latestPost(doc: BlogPost)(using ctx: PageContext, prismic: lila.blog.BlogApi.Context) =
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
              cls      := "button",
              href     := routes.Blog.show(doc.id, doc.slug, ref = prismic.maybeRef),
              dataIcon := licon.PlayTriangle
            )(
              " Continue reading this post"
            )
          )
        )
      )
    )
