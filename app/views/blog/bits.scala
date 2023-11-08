package views.html.blog

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.blog.{ BlogPost, MiniPost }
import lila.common.String.html.richText

import controllers.routes

object bits:

  def menu(year: Option[Int], active: Option[String])(using ctx: Context) =
    views.html.site.bits.pageMenuSubnav(
      cls := "force-ltr",
      ctx.kid.no option a(
        cls  := active.has("community").option("active"),
        href := langHref(routes.Ublog.communityAll())
      )("Community blogs"),
      ctx.kid.no option a(cls := active.has("topics").option("active"), href := routes.Ublog.topics)(
        "Blog topics"
      ),
      (ctx.isAuth && ctx.kid.no) option a(
        cls  := active.has("friends").option("active"),
        href := routes.Ublog.friends()
      )("Friends blogs"),
      ctx.kid.no option a(cls := active.has("liked").option("active"), href := routes.Ublog.liked())(
        "Liked blog posts"
      ),
      ctx.me
        .ifTrue(ctx.kid.no)
        .map: me =>
          a(cls := active.has("mine").option("active"), href := routes.Ublog.index(me.username))("My blog"),
      a(cls := active.has("lichess").option("active"), href := routes.Blog.index())("Lichess blog"),
      year.isDefined || active.has("lichess") option lila.blog.allYears.map: y =>
        a(cls := (year has y).option("active"), href := routes.Blog.year(y))(y)
    )

  private[blog] def postCard(
      post: MiniPost,
      postClass: Option[String] = None,
      header: Tag = h2
  )(using Context) =
    a(cls := postClass)(href := routes.Blog.show(post.id, post.slug))(
      st.img(src := post.image),
      div(cls := "content")(
        header(cls := "title")(post.title),
        span(post.shortlede),
        semanticDate(post.date)
      )
    )

  private[blog] def metas(post: BlogPost)(using ctx: Context, prismic: lila.blog.BlogApi.Context) =
    div(cls := "meta-headline")(
      div(cls := "meta")(
        post.date.map: date =>
          span(cls := "text", dataIcon := licon.Clock)(semanticDate(date)),
        post.author.map: author =>
          span(cls := "text", dataIcon := licon.User)(richText(author)),
        post.category.map: categ =>
          span(cls := "text", dataIcon := licon.Star)(categ)
      ),
      strong(cls := "headline")(post.getHtml("blog.shortlede", prismic.linkResolver).map(raw))
    )

  private[blog] def csp(using PageContext) = defaultCsp.withPrismic(isGranted(_.Prismic)).some
