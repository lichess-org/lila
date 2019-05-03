package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object show {

  def apply(doc: io.prismic.Document)(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) =
    views.html.base.layout(
      title = s"${~doc.getText("blog.title")} | Blog",
      moreJs = jsAt("compiled/embed-analyse.js"),
      openGraph = lila.app.ui.OpenGraph(
        `type` = "article",
        image = doc.getImage("blog.image", "main").map(_.url),
        title = ~doc.getText("blog.title"),
        url = s"$netBaseUrl${routes.Blog.show(doc.id, doc.slug).url}",
        description = ~doc.getText("blog.shortlede")
      ).some,
      moreCss = cssTag("blog"),
      csp = bits.csp
    )(
        main(cls := s"blog page-small box post ${~doc.getText("blog.cssClasses")}")(
          h1(
            a(href := routes.Blog.index(), dataIcon := "I", cls := "text"),
            doc.getText("blog.title")
          ),
          bits.metas(doc),
          doc.getImage("blog.image", "main").map { img =>
            div(cls := "illustration")(st.img(src := img.url))
          },
          div(cls := "body embed_analyse")(
            doc.getHtml("blog.body", prismic.linkResolver).map(lila.blog.Youtube.fixStartTimes).map(lila.blog.ProtocolFix.remove).map(raw)
          ),
          NotForKids {
            div(cls := "footer")(
              if (prismic.maybeRef.isEmpty) {
                (doc.getDate("blog.date").exists(_.value.toDateTimeAtStartOfDay isAfter org.joda.time.DateTime.now.minusWeeks(2))) option
                  a(href := routes.Blog.discuss(doc.id), cls := "button text discuss", dataIcon := "d")("Discuss this blog post in the forum")
              } else p("This is a preview.")
            )
          }
        )
      )
}
