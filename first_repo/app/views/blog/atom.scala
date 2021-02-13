package views.html.blog

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.common.config.BaseUrl

import controllers.routes

object atom {

  def apply(
      pager: Paginator[io.prismic.Document],
      baseUrl: BaseUrl
  )(implicit prismic: lila.blog.BlogApi.Context) =
    frag(
      raw("""<?xml version="1.0" encoding="UTF-8"?>"""),
      raw(
        """<feed xml:lang="en-US" xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">"""
      ),
      tag("id")(s"$baseUrl${routes.Blog.index()}"),
      link(rel := "alternate", tpe := "text/html", href := s"$baseUrl${routes.Blog.index()}"),
      link(rel := "self", tpe := "application/atom+xml", href := s"$baseUrl${routes.Blog.atom}"),
      tag("title")("lichess.org blog"),
      tag("updated")(pager.currentPageResults.headOption.flatMap(atomDate("blog.date"))),
      pager.currentPageResults.map { doc =>
        tag("entry")(
          tag("id")(s"$baseUrl${routes.Blog.show(doc.id, doc.slug)}"),
          tag("published")(atomDate("blog.date")(doc)),
          tag("updated")(atomDate("blog.date")(doc)),
          link(
            rel := "alternate",
            tpe := "text/html",
            href := s"$baseUrl${routes.Blog.show(doc.id, doc.slug)}"
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
      },
      raw("</feed>")
    )
}
