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
      raw("""<?xml version="1.0" encoding="utf-8"?>"""),
      raw(
        """<feed xml:lang="en-US" xmlns="http://www.w3.org/2005/Atom" xmlns:media="http://search.yahoo.com/mrss/">"""
      ),
      tag("id")(s"$baseUrl${routes.Blog.index()}"),
      link(rel := "alternate", tpe := "text/html", href            := s"$baseUrl${routes.Blog.index()}"),
      link(rel := "self", tpe      := "application/atom+xml", href := s"$baseUrl${routes.Blog.atom}"),
      tag("title")("lishogi.org blog"),
      tag("updated")(pager.currentPageResults.headOption.flatMap(atomDate("blog.date"))),
      pager.currentPageResults.flatMap(doc => lila.blog.FullPost.fromDocument("blog")(doc)).map { post =>
        tag("entry")(
          tag("id")(s"$baseUrl${routes.Blog.show(post.id)}"),
          tag("published")(atomDate(post.date)),
          tag("updated")(atomDate(post.date)),
          link(
            rel  := "alternate",
            tpe  := "text/html",
            href := s"$baseUrl${routes.Blog.show(post.id)}"
          ),
          tag("title")(post.title),
          tag("category")(
            tag("term")(post.category),
            tag("label")(slugify(post.category))
          ),
          tag("content")(tpe := "html")(
            post.doc.getText(s"${post.coll}.shortlede"),
            "<br>", // yes, scalatags encodes it.
            st.img(src := post.image).render,
            "<br>",
            post.doc
              .getHtml(s"${post.coll}.body", prismic.linkResolver)
              .map(lila.blog.Youtube.fixStartTimes)
              .map(lila.blog.BlogTransform.addProtocol)
          ),
          tag("tag")("media:thumbnail")(attr("url") := post.image),
          tag("author")(tag("name")(post.author))
        )
      },
      raw("</feed>")
    )
}
