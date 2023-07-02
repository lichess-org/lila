package views.html.blog

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object show {

  def apply(post: lila.blog.FullPost)(implicit ctx: Context, prismic: lila.blog.BlogApi.Context) =
    views.html.base.layout(
      title = post.title,
      moreJs = jsAt("compiled/embed-analyse.js"),
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = post.image.some,
          title = post.title,
          url = s"$netBaseUrl${routes.Blog.show(post.id).url}",
          description = ~post.doc.getText(s"${post.coll}.shortlede")
        )
        .some,
      moreCss = cssTag("blog"),
      csp = bits.csp,
      withHrefLangs = post.allLangIds.map { ids =>
        lila.i18n.LangList
          .Custom(
            Map(
              "en" -> routes.Blog.show(ids.en).url,
              "ja" -> routes.Blog.show(ids.ja).url
            )
          )
      }
    )(
      main(cls := "page-menu page-small")(
        bits.menu(none, false),
        div(cls := s"blog page-menu__content box post")(
          h1(post.title),
          bits.metas(post),
          div(cls := "illustration")(st.img(src := post.image)),
          div(cls := "body embed_analyse")(
            post.doc
              .getHtml(s"${post.coll}.body", prismic.linkResolver)
              .map(lila.blog.Youtube.fixStartTimes)
              .map(lila.blog.BlogTransform.removeProtocol)
              .map(lila.blog.BlogTransform.markdown.apply)
              .map(raw)
          ),
          ctx.noKid option
            div(cls := "footer")(
              if (prismic.maybeRef.isEmpty) {
                (
                  post.date isAfter org.joda.time.DateTime.now.minusWeeks(2)
                ) option
                  a(href := routes.Blog.discuss(post.doc.id), cls := "button text discuss", dataIcon := "d")(
                    trans.discussBlogForum()
                  )
              } else p("This is a preview."),
              views.html.base.bits.connectLinks
            )
        )
      )
    )
}
