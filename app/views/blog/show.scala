package views.html.blog

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.blog.BlogPost

import controllers.routes

object show:

  def apply(post: BlogPost)(using ctx: PageContext, prismic: lila.blog.BlogApi.Context) =
    val postTitle = ~post.title
    views.html.base.layout(
      title = s"$postTitle | Blog",
      moreJs = jsModule("expandText"),
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = post.getImage("blog.image", "main").map(_.url),
          title = postTitle,
          url = s"$netBaseUrl${routes.Blog.show(post.id, post.slug).url}",
          description = post.shortlede
        )
        .some,
      moreCss = cssTag("blog"),
      csp = bits.csp.map(_.withInlineIconFont)
    ):
      main(cls := "page-menu page-small")(
        bits.menu(none, "lichess".some),
        div(cls := s"blog page-menu__content box post force-ltr ${~post.getText("blog.cssClasses")}")(
          h1(cls := "box__top")(postTitle),
          bits.metas(post),
          post.getImage("blog.image", "main").map { img =>
            div(cls := "illustration")(st.img(src := img.url))
          },
          div(cls := "body expand-text")(
            Html
              .from(post.getHtml("blog.body", prismic.linkResolver))
              .map(lila.blog.Youtube.augmentEmbeds)
              .map(lila.blog.BlogTransform.removeProtocol)
              .map(lila.blog.BlogTransform.markdown.apply)
              .map(env.blog.api.expand)
              .map(rawHtml)
          ),
          ctx.kid.no option
            div(cls := "footer")(
              if prismic.maybeRef.isEmpty then
                post.date
                  .exists:
                    _.atStartOfDay.instant isAfter nowInstant.minusWeeks(2)
                  .option:
                    a(
                      href     := routes.Blog.discuss(post.id),
                      cls      := "button text discuss",
                      dataIcon := licon.BubbleConvo
                    ):
                      "Discuss this blog post in the forum"
              else p("This is a preview."),
              views.html.base.bits.connectLinks,
              p(cls := "patron-info")(a(href := routes.Plan.index)(trans.lichessPatronInfo()))
            )
        )
      )
