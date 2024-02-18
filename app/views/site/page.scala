package views.html.site

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.cms.CmsPage

object page:

  def lone(p: CmsPage.Render)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("page"),
      title = p.title,
      moreJs = p.key == CmsPage.Key("fair-play") option embedJsUnsafeLoadThen("""$('.slist td').each(function() {
if (this.innerText == 'YES') this.style.color = 'green'; else if (this.innerText == 'NO') this.style.color = 'red';
})""")
    ):
      main(cls := "page-small box box-pad page force-ltr")(pageContent(p))

  def withMenu(active: String, p: CmsPage.Render)(using PageContext) =
    layout(
      title = p.title,
      active = active,
      contentCls = "page box box-pad force-ltr",
      moreCss = cssTag("page")
    ):
      pageContent(p)

  def pageContent(p: CmsPage.Render)(using Context) = frag(
    h1(cls := "box__top")(p.title),
    div(cls := "body")(views.html.cms.render(p))
  )

  def source(p: CmsPage.Render)(using PageContext) =
    layout(
      title = p.title,
      active = "source",
      moreCss = cssTag("source"),
      contentCls = "page force-ltr",
      moreJs = embedJsUnsafeLoadThen:
        """$('#asset-version-date').text(site.info.date);
$('#asset-version-commit').attr('href', 'https://github.com/lichess-org/lila/commits/' + site.info.commit).find('pre').text(site.info.commit.substr(0, 7));
$('#asset-version-upcoming').attr('href', 'https://github.com/lichess-org/lila/compare/' + site.info.commit + '...master').find('pre').text('...');
$('#asset-version-message').text(site.info.message);"""
    ):
      val commit = env.appVersionCommit | "???"
      frag(
        st.section(cls := "box")(
          h1(cls := "box__top")(p.title),
          table(cls := "slist slist-pad", id := "version")(
            thead(
              tr(
                th(colspan := 3)("Current versions"),
                th(colspan := 2)("Last boot: ", momentFromNow(lila.common.Uptime.startedAt))
              )
            ),
            tbody(
              tr(
                td("Server"),
                td(env.appVersionDate),
                td(a(href := s"https://github.com/lichess-org/lila/commits/$commit")(pre(commit.take(7)))),
                td(env.appVersionMessage),
                td(a(href := s"https://github.com/lichess-org/lila/compare/$commit...master")(pre("...")))
              ),
              tr(
                td("Assets"),
                td(id := "asset-version-date"),
                td(a(id := "asset-version-commit")(pre)),
                td(id := "asset-version-message"),
                td(a(id := "asset-version-upcoming")(pre("...")))
              )
            )
          )
        ),
        st.section(cls := "box box-pad body")(views.html.cms.render(p)),
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
                st.title := "Copy code",
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
                st.title := "Copy code",
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
              a(href := routes.Study.allDefault())("a study"),
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
          a(activeCls("news"), href := routes.Feed.index(1))("Lichess updates"),
          a(activeCls("faq"), href := routes.Main.faq)(trans.faq.faqAbbreviation()),
          a(activeCls("contact"), href := routes.Main.contact)(trans.contact.contact()),
          a(activeCls("tos"), href := routes.Cms.tos)(trans.termsOfService()),
          a(activeCls("privacy"), href := "/privacy")(trans.privacy()),
          a(activeCls("master"), href := routes.Cms.master)("Title verification"),
          sep,
          a(activeCls("source"), href := routes.Cms.source)(trans.sourceCode()),
          a(activeCls("help"), href := routes.Cms.help)(trans.contribute()),
          a(activeCls("changelog"), href := routes.Cms.menuPage("changelog"))("Changelog"),
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
