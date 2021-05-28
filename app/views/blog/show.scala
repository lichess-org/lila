package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object show {

  def apply(doc: io.prismic.Document)(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) =
    views.html.base.layout(
      title = s"${~doc.getText("blog.title")} | Blog",
      moreJs = jsModule("expandText"),
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = doc.getImage("blog.image", "main").map(_.url),
          title = ~doc.getText("blog.title"),
          url = s"$netBaseUrl${routes.Blog.show(doc.id, doc.slug).url}",
          description = ~doc.getText("blog.shortlede")
        )
        .some,
      moreCss = cssTag("blog"),
      csp = bits.csp
    )(
      main(cls := "page-menu page-small")(
        bits.menu(none, hasActive = false),
        div(cls := s"blog page-menu__content box post ${~doc.getText("blog.cssClasses")}")(
          h1(doc.getText("blog.title")),
          bits.metas(doc),
          doc.getImage("blog.image", "main").map { img =>
            div(cls := "illustration")(st.img(src := img.url))
          },
          div(cls := "body expand-text")(
            doc
              .getHtml("blog.body", prismic.linkResolver)
              .map(lila.blog.Youtube.fixStartTimes)
              .map(lila.blog.BlogTransform.removeProtocol)
              .map(lila.blog.BlogTransform.markdown.apply)
              .map(raw)
          ),
          ctx.noKid option
            div(cls := "footer")(
              if (prismic.maybeRef.isEmpty) {
                (doc
                  .getDate("blog.date")
                  .exists(
                    _.value.toDateTimeAtStartOfDay isAfter org.joda.time.DateTime.now.minusWeeks(2)
                  )) option
                  a(href := routes.Blog.discuss(doc.id), cls := "button text discuss", dataIcon := "d")(
                    "Discuss this blog post in the forum"
                  )
              } else p("This is a preview."),
              views.html.base.bits.connectLinks,
              p(cls := "patron-info")(a(href := routes.Plan.index)(trans.lichessPatronInfo()))
            )
        )
      )
    )
}
