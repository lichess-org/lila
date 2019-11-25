package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.blog.MiniPost
import lila.common.paginator.Paginator

import controllers.routes

object all {

  def apply(year: Int, posts: List[MiniPost])(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) =
    views.html.base.layout(
      title = s"Blog posts from $year",
      moreCss = cssTag("blog.cards"),
      csp = bits.csp,
      moreJs = infiniteScrollTag
    )(
        main(cls := "page-menu")(
          st.nav(cls := "page-menu__menu subnav")(
            lila.blog.allYears map { y =>
              a(cls := (y == year).option("active"), href := routes.Blog.year(y))(y)
            }
          ),
          div(id := "chat-panic", cls := "page-menu__content blog-cards box")(
            div(cls := "box__top")(
              h1(s"Blog posts from $year"),
              a(cls := "atom", href := routes.Blog.atom, dataIcon := "3")
            ),
            st.section(
              div(cls := "list")(
                posts.map { post =>
                  a(href := routes.Blog.show(post.id, post.slug))(
                    st.img(src := post.image),
                    div(cls := "content")(
                      h2(cls := "title")(post.title),
                      span(post.shortlede),
                      semanticDate(post.date)
                    )
                  )
                }
              )
            )
          )
        )
      )
}
