package views.html.blog

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.blog.{ BlogPost, MiniPost }
import java.time.LocalDate

object atom:

  import views.html.base.atom.{ atomDate, category }

  def apply(pager: Paginator[BlogPost])(using prismic: lila.blog.BlogApi.Context) =
    views.html.base.atom(
      elems = pager.currentPageResults,
      htmlCall = routes.Blog.index(),
      atomCall = routes.Blog.atom,
      title = "lichess.org blog",
      updated = pager.currentPageResults.headOption.flatMap(_.date).map(_.atStartOfDay.instant)
    ): doc =>
      frag(
        tag("id")(s"$netBaseUrl${routes.Blog.show(doc.id, doc.slug)}"),
        tag("published")(doc.date map atomDate),
        tag("updated")(doc.date map atomDate),
        link(
          rel  := "alternate",
          tpe  := "text/html",
          href := s"$netBaseUrl${routes.Blog.show(doc.id, doc.slug)}"
        ),
        tag("title")(doc.getText("blog.title")),
        category(
          term = slugify(~doc.getText("blog.category")),
          label = ~doc.getText("blog.category")
        ),
        tag("content")(tpe := "html")(
          doc.getText("blog.shortlede"),
          "<br>", // yes, scalatags encodes it.
          doc.getImage("blog.image", "main").map { img =>
            st.img(src := img.url).render
          },
          "<br>",
          Html
            .from(doc.getHtml("blog.body", prismic.linkResolver))
            .map(lila.blog.Youtube.augmentEmbeds)
            .map(lila.blog.BlogTransform.addProtocol)
        ),
        tag("tag")("media:thumbnail")(attr("url") := doc.getImage(s"blog.image", "main").map(_.url)),
        tag("author")(tag("name")(doc.getText("blog.author")))
      )
