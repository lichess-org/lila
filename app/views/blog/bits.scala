package views.html.blog

import lila.api.Context
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.blog.MiniPost
import lila.common.String.html.richText

import controllers.routes

object bits:

  def menu(year: Option[Int], active: Option[String])(implicit ctx: Context) =
    st.nav(cls := "page-menu__menu subnav force-ltr")(
      a(cls := active.has("community").option("active"), href := langHref(routes.Ublog.communityAll()))(
        "Community blogs"
      ),
      a(cls := active.has("topics").option("active"), href := routes.Ublog.topics)("Blog topics"),
      ctx.isAuth option a(cls := active.has("friends").option("active"), href := routes.Ublog.friends())(
        "Friends blogs"
      ),
      a(cls := active.has("liked").option("active"), href := routes.Ublog.liked())("Liked blog posts"),
      ctx.me map { me =>
        a(cls := active.has("mine").option("active"), href := routes.Ublog.index(me.username))("My blog")
      },
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
  )(using ctx: Context, prismic: lila.blog.BlogApi.Context) =
    div(cls := "meta-headline")(
      div(cls := "meta")(
        doc.getDate("blog.date").map { date =>
          span(cls := "text", dataIcon := "")(semanticDate(date.value))
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

  private[blog] def csp(using Context) = defaultCsp.withPrismic(isGranted(_.Prismic)).some
