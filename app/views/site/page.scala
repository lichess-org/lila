package views.html.site

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import io.prismic.{ Document, DocumentLinkResolver }

object page:

  def lone(doc: Document, resolver: DocumentLinkResolver)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("page"),
      title = ~doc.getText("doc.title"),
      moreJs = doc.slugs.has("fair-play") option fairPlayJs
    ):
      main(cls := "page-small box box-pad page force-ltr")(pageContent(doc, resolver))

  private def fairPlayJs(using PageContext) = embedJsUnsafeLoadThen("""$('.slist td').each(function() {
if (this.innerText == 'YES') this.style.color = 'green'; else if (this.innerText == 'NO') this.style.color = 'red';
})""")

  def withMenu(active: String, doc: Document, resolver: DocumentLinkResolver)(using PageContext) =
    layout(
      title = ~doc.getText("doc.title"),
      active = active,
      contentCls = "page box box-pad force-ltr",
      moreCss = cssTag("page")
    ):
      pageContent(doc, resolver)

  def pageContent(doc: Document, resolver: DocumentLinkResolver) = frag(
    h1(cls := "box__top")(doc.getText("doc.title")),
    div(cls := "body")(
      Html
        .from(doc.getHtml("doc.content", resolver))
        .map(lila.blog.BlogTransform.markdown.apply)
        .map(rawHtml)
    )
  )

  def source(doc: Document, resolver: DocumentLinkResolver)(using PageContext) =
    val title = ~doc.getText("doc.title")
    layout(
      title = title,
      active = "source",
      moreCss = frag(cssTag("source")),
      contentCls = "page force-ltr",
      moreJs = embedJsUnsafeLoadThen(
        """$('#asset-version-date').text(lichess.info.date);
$('#asset-version-commit').attr('href', 'https://github.com/lichess-org/lila/commits/' + lichess.info.commit).find('pre').text(lichess.info.commit.substr(0, 12));
$('#asset-version-message').text(lichess.info.message);"""
      )
    ):
      frag(
        st.section(cls := "box box-pad body")(
          h1(cls := "box__top")(title),
          raw(~doc.getHtml("doc.content", resolver))
        ),
        br,
        st.section(cls := "box")(
          h1(id := "version", cls := "box__top")("lila version"),
          table(cls := "slist slist-pad")(
            env.appVersionDate zip env.appVersionCommit zip env.appVersionMessage map {
              case ((date, commit), message) =>
                tr(
                  td("Server"),
                  td(date),
                  td(a(href := s"https://github.com/lichess-org/lila/commits/$commit")(pre(commit.take(12)))),
                  td(message)
                )
            },
            tr(
              td("Assets"),
              td(id := "asset-version-date"),
              td(a(id := "asset-version-commit")(pre)),
              td(id := "asset-version-message")
            ),
            tr(
              td("Boot"),
              td(colspan := 3)(momentFromNow(lila.common.Uptime.startedAt))
            )
          )
        ),
        br,
        st.section(cls := "box")(freeJs())
      )

  def webmasters(using PageContext) =
    val parameters = frag(
      p("Parameters:"),
      ul(
        li(strong("theme"), ": ", lila.pref.Theme.all.map(_.name).mkString(", ")),
        li(strong("pieceSet"), ": ", lila.pref.PieceSet.all.map(_.name).mkString(", ")),
        li(strong("bg"), ": light, dark, system")
      )
    )
    layout(
      title = "Webmasters",
      active = "webmasters",
      moreCss = cssTag("page"),
      contentCls = "page force-ltr"
    ):
      frag(
        st.section(cls := "box box-pad developers body")(
          h1(cls := "box__top")("HTTP API"),
          p(
            raw(
              """Lichess exposes a RESTish HTTP/JSON API that you are welcome to use. Read the <a href="/api" class="blue">HTTP API documentation</a>."""
            )
          )
        ),
        br,
        st.section(cls := "box box-pad developers body") {
          val args = """style="width: 400px; height: 444px;" allowtransparency="true" frameborder="0""""
          frag(
            h1(cls := "box__top", id := "embed-tv")("Embed Lichess TV in your site"),
            div(cls := "center")(raw(s"""<iframe src="/tv/frame?theme=brown&bg=dark" $args></iframe>""")),
            p("Add the following HTML to your site:"),
            p(cls := "copy-zone")(
              input(
                id    := "tv-embed-src",
                cls   := "copyable autoselect",
                value := s"""<iframe src="$netBaseUrl/tv/frame?theme=brown&bg=dark" $args></iframe>"""
              ),
              button(
                title    := "Copy code",
                cls      := "copy button",
                dataRel  := "tv-embed-src",
                dataIcon := licon.Link
              )
            ),
            parameters
          )
        },
        br,
        st.section(cls := "box box-pad developers body") {
          val args = """style="width: 400px; height: 444px;" allowtransparency="true" frameborder="0""""
          frag(
            h1(cls := "box__top", id := "embed-puzzle")("Embed the daily puzzle in your site"),
            div(cls := "center")(
              raw(s"""<iframe src="/training/frame?theme=brown&bg=dark" $args></iframe>""")
            ),
            p("Add the following HTML to your site:"),
            p(cls := "copy-zone")(
              input(
                id    := "puzzle-embed-src",
                cls   := "copyable autoselect",
                value := s"""<iframe src="$netBaseUrl/training/frame?theme=brown&bg=dark" $args></iframe>"""
              ),
              button(
                title    := "Copy code",
                cls      := "copy button",
                dataRel  := "puzzle-embed-src",
                dataIcon := licon.Link
              )
            ),
            parameters,
            p("The text is automatically translated to your visitor's language."),
            p(
              "Alternatively, you can ",
              a(href := routes.Main.dailyPuzzleSlackApp)("post the puzzle in your slack workspace"),
              "."
            )
          )
        },
        br,
        st.section(cls := "box box-pad developers body") {
          val args = """style="width: 600px; height: 397px;" frameborder="0""""
          frag(
            h1(cls := "box__top", id := "embed-study")("Embed a chess analysis in your site"),
            raw(s"""<iframe src="/study/embed/XtFCFYlM/GCUTf2Jk?bg=auto&theme=auto" $args></iframe>"""),
            p(
              "Create ",
              a(href := routes.Study.allDefault(1))("a study"),
              ", then click the share button to get the HTML code for the current chapter."
            ),
            parameters,
            p("The text is automatically translated to your visitor's language.")
          )
        },
        br,
        st.section(cls := "box box-pad developers body") {
          val args = """style="width: 600px; height: 397px;" frameborder="0""""
          frag(
            h1(cls := "box__top")("Embed a chess game in your site"),
            raw(s"""<iframe src="/embed/game/MPJcy1JW?bg=auto&theme=auto" $args></iframe>"""),
            p(
              raw("""On a game analysis page, click the <em>"FEN &amp; PGN"</em> tab at the bottom, then """),
              "\"",
              em(trans.embedInYourWebsite(), "\".")
            ),
            parameters,
            p("The text is automatically translated to your visitor's language.")
          )
        }
      )

  def layout(
      title: String,
      active: String,
      contentCls: String = "",
      moreCss: Frag = emptyFrag,
      moreJs: Frag = emptyFrag
  )(body: Frag)(using PageContext) =
    views.html.base.layout(
      title = title,
      moreCss = moreCss,
      moreJs = moreJs
    ):
      val sep                  = div(cls := "sep")
      val external             = frag(" ", i(dataIcon := licon.ExternalArrow))
      def activeCls(c: String) = cls := active.activeO(c)
      main(cls := "page-menu")(
        views.html.site.bits.pageMenuSubnav(
          a(activeCls("about"), href := "/about")(trans.aboutX("lichess.org")),
          a(activeCls("faq"), href := routes.Main.faq)(trans.faq.faqAbbreviation()),
          a(activeCls("contact"), href := routes.Main.contact)(trans.contact.contact()),
          a(activeCls("tos"), href := routes.ContentPage.tos)(trans.termsOfService()),
          a(activeCls("privacy"), href := "/privacy")(trans.privacy()),
          a(activeCls("master"), href := routes.ContentPage.master)("Title verification"),
          sep,
          a(activeCls("source"), href := routes.ContentPage.source)(trans.sourceCode()),
          a(activeCls("help"), href := routes.ContentPage.help)(trans.contribute()),
          a(activeCls("changelog"), href := routes.ContentPage.menuBookmark("changelog"))("Changelog"),
          a(activeCls("thanks"), href := "/thanks")(trans.thankYou()),
          sep,
          a(activeCls("webmasters"), href := routes.Main.webmasters)(trans.webmasters()),
          a(activeCls("database"), href := "https://database.lichess.org")(trans.database(), external),
          a(activeCls("api"), href := routes.Api.index)("API", external),
          sep,
          a(activeCls("lag"), href := routes.Main.lag)(trans.lag.isLichessLagging()),
          a(activeCls("ads"), href := "/ads")("Block ads")
        ),
        div(cls := s"page-menu__content $contentCls")(body)
      )
