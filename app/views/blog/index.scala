package views.html.blog

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator

import controllers.routes

object index {

  def apply(pager: Paginator[io.prismic.Document])(implicit ctx: Context, prismic: lidraughts.blog.BlogApi.Context) =
    views.html.base.layout(
      title = "Blog",
      moreCss = cssTag("blog"),
      csp = bits.csp,
      moreJs = infiniteScrollTag
    )(
        main(cls := "blog list page-small box")(
          div(cls := "box__top")(
            h1("Lidraughts Official Blog"),
            a(cls := "atom", href := routes.Blog.atom, dataIcon := "3")
          ),
          div(cls := "list infinitescroll")(
            pager.currentPageResults.map { doc =>
              st.article(cls := "paginated")(
                doc.getText("blog.title").map { title =>
                  h2(a(href := routes.Blog.show(doc.id, doc.slug, prismic.maybeRef))(title))
                },
                bits.metas(doc),
                doc.getImage("blog.image", "main").map { img =>
                  div(cls := "illustration")(
                    a(href := routes.Blog.show(doc.id, doc.slug, ref = prismic.maybeRef))(st.img(src := img.url))
                  )
                },
                div(cls := "body")(
                  doc.getStructuredText("blog.body").map { body =>
                    raw(lidraughts.blog.BlogApi.extract(body))
                  },
                  p(cls := "more")(
                    a(cls := "button", href := routes.Blog.show(doc.id, doc.slug, ref = prismic.maybeRef), dataIcon := "G")(
                      " Continue reading this post"
                    )
                  )
                )
              )
            },
            pagerNext(pager, np => routes.Blog.index(np, none).url)
          )
        )
      )
}
