package views.html.blog

import controllers.routes

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

object atom {

  import views.html.base.atom.atomDate

  def apply(
      pager: Paginator[io.prismic.Document]
  )(implicit prismic: lila.blog.BlogApi.Context) =
    views.html.base.atom(
      elems = pager.currentPageResults,
      htmlCall = routes.Blog.index(),
      atomCall = routes.Blog.atom,
      title = "lichess.org blog",
      updated = pager.currentPageResults.headOption flatMap docDate
    ) { doc =>
      frag(
        tag("id")(s"$netBaseUrl${routes.Blog.show(doc.id, doc.slug)}"),
        tag("published")(docDate(doc) map atomDate),
        tag("updated")(docDate(doc) map atomDate),
        link(
          rel := "alternate",
          tpe := "text/html",
          href := s"$netBaseUrl${routes.Blog.show(doc.id, doc.slug)}"
        ),
        tag("title")(doc.getText("blog.title")),
        tag("category")(
          tag("term")(doc.getText("blog.category")),
          tag("label")(slugify(~doc.getText("blog.category")))
        ),
        tag("content")(tpe := "html")(
          doc.getText("blog.shortlede"),
          "<br>", // yes, scalatags encodes it.
          doc.getImage("blog.image", "main").map { img =>
            st.img(src := img.url).render
          },
          "<br>",
          doc
            .getHtml("blog.body", prismic.linkResolver)
            .map(lila.blog.Youtube.fixStartTimes)
            .map(lila.blog.BlogTransform.addProtocol)
        ),
        tag("tag")("media:thumbnail")(attr("url") := doc.getImage(s"blog.image", "main").map(_.url)),
        tag("author")(tag("name")(doc.getText("blog.author")))
      )
    }

  def docDate(doc: io.prismic.Document) =
    doc getDate "blog.date" map (_.value.toDateTimeAtStartOfDay)
}
