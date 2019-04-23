package views.html.blog

import play.api.mvc.RequestHeader

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object atom {

  def apply(pager: Paginator[io.prismic.Document])(implicit req: RequestHeader) = frag(
    raw("""<?xml version="1.0" encoding="UTF-8"?>"""),
    raw("""<feed xml:lang="en-US" xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">"""),
    tag("id")(routes.Blog.index().absoluteURL(true)),
    link(rel := "alternate", tpe := "text/html", href := routes.Blog.index().absoluteURL(true)),
    link(rel := "self", tpe := "application/atom+xml", href := routes.Blog.atom().absoluteURL(true)),
    tag("title")("lichess.org blog"),
    tag("updated")(pager.currentPageResults.headOption.flatMap(atomDate("blog.date"))),
    pager.currentPageResults.map { doc =>
      tag("entry")(
        tag("id")(routes.Blog.show(doc.id, doc.slug).absoluteURL(true)),
        tag("published")(atomDate("blog.date")(doc)),
        tag("updated")(atomDate("blog.date")(doc)),
        link(rel := "alternate", tpe := "text/html", href := routes.Blog.show(doc.id, doc.slug).absoluteURL(true)),
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
          lila.blog.ProtocolFix.add(doc.getStructuredText("blog.body") ?? lila.blog.BlogApi.extract)
        ),
        tag("tag")("media:thumbnail")(attr("url") := doc.getImage(s"blog.image", "main").map(_.url)),
        tag("author")(tag("name")(doc.getText("blog.author")))
      )
    },
    raw("</feed>")
  )
}
