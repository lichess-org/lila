package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.hub.actorApi.timeline.BlogPost

import controllers.routes

object show {

  def apply(doc: io.prismic.Document)(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) =
    views.html.base.layout(
      title = s"${~doc.getText("blog.title")} | Blog",
      moreJs = jsAt("compiled/embed-analyse.js"),
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
        bits.menu(none, false),
        div(cls := s"blog page-menu__content box post ${~doc.getText("blog.cssClasses")}")(
          h1(doc.getText("blog.title")),
          bits.metas(doc),
          doc.getImage("blog.image", "main").map { img =>
            div(cls := "illustration")(st.img(src := img.url))
          },
          div(cls := "body embed_analyse")(
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
                if (doc
                  .getDate("blog.date")
                  .exists(
                    _.value.toDateTimeAtStartOfDay isAfter org.joda.time.DateTime.now.minusWeeks(2)
                  )) {
                    // if locale is in japanese, check if un-translated by searching timeline for same-titled blog post but in english locale. bit hacky but works.
                    val enBlogId = env.timeline.entryApi
                      .broadcast.cacheGet map {
                        _.decode.map {
                          case BlogPost(id, slug, _, _) => Some(Map("id" -> id, "slug" -> slug))
                          case _ => None
                        } get
                      } filter { _ ??
                        { blogMap =>
                          blogMap("id") != doc.id && blogMap("slug") == doc.slug
                        }
                      } map { _.get("id") } headOption
                    // if the japanese blog was not translated yet, point the forum post link to the english blog's forum post instead.
                    val idForDiscuss = enBlogId getOrElse doc.id
                    a(href := routes.Blog.discuss(idForDiscuss), cls := "button text discuss", dataIcon := "d")(
                      trans.discussBlogForum()
                    )
                }
              } else p("This is a preview."),
              views.html.base.bits.connectLinks
            )
        )
      )
    )
}
