package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.blog.MiniPost
import lila.common.String.html.richText

import controllers.routes

object bits {

  private[blog] def menu(year: Option[Int], hasActive: Boolean = true) =
    st.nav(cls := "page-menu__menu subnav")(
      a(cls := (year.isEmpty && hasActive).option("active"), href := routes.Blog.index())("Latest"),
      lila.blog.allYears map { y =>
        a(cls := (year has y).option("active"), href := routes.Blog.year(y))(y)
      }
    )

  private[blog] def postCard(
      post: MiniPost,
      postClass: Option[String] = None,
      header: Tag = h2
  )(implicit ctx: Context) =
    a(cls := postClass)(href := routes.Blog.show(post.id, post.slug))(
      st.img(src := post.image),
      div(cls := "content")(
        header(cls := "title")(post.title),
        span(post.shortlede),
        semanticDate(post.date)
      )
    )

  private[blog] def metas(
      doc: io.prismic.Document
  )(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) =
    div(cls := "meta-headline")(
      div(cls := "meta")(
        doc.getDate("blog.date").map { date =>
          span(cls := "text", dataIcon := "p")(semanticDate(date.value.toDateTimeAtStartOfDay))
        },
        doc.getText("blog.author").map { author =>
          span(cls := "text", dataIcon := "r")(richText(author))
        },
        doc.getText("blog.category").map { categ =>
          span(cls := "text", dataIcon := "t")(categ)
        }
      ),
      strong(cls := "headline")(doc.getHtml("blog.shortlede", prismic.linkResolver).map(raw))
    )

  private[blog] def csp(implicit ctx: Context) = defaultCsp.withPrismic(isGranted(_.Prismic)).some
}
