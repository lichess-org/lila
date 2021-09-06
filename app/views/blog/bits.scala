package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.blog.MiniPost
import lila.common.String.html.richText

import controllers.routes

object bits {

  def menu(year: Option[Int], active: Option[String]) =
    st.nav(cls := "page-menu__menu subnav")(
      a(cls := active.has("friends").option("active"), href := routes.Ublog.friends())("Friends blogs"),
      a(cls := active.has("community").option("active"), href := routes.Ublog.community())("Community blogs"),
      a(cls := active.has("lichess").option("active"), href := routes.Blog.index())("Lichess blog"),
      year.isDefined || active.has("lichess") option lila.blog.allYears.map { y =>
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
          span(cls := "text", dataIcon := "")(semanticDate(date.value.toDateTimeAtStartOfDay))
        },
        doc.getText("blog.author").map { author =>
          span(cls := "text", dataIcon := "")(richText(author))
        },
        doc.getText("blog.category").map { categ =>
          span(cls := "text", dataIcon := "")(categ)
        }
      ),
      strong(cls := "headline")(doc.getHtml("blog.shortlede", prismic.linkResolver).map(raw))
    )

  private[blog] def csp(implicit ctx: Context) = defaultCsp.withPrismic(isGranted(_.Prismic)).some
}
