package views.html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object help {

  def page(active: String, doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) = {
    val title = ~doc.getText("doc.title")
    layout(
      title = title,
      active = active,
      contentCls = "page box box-pad",
      moreCss = cssTag("page")
    )(frag(
        h1(title),
        div(cls := "body")(raw(~doc.getHtml("doc.content", resolver)))
      ))
  }

  def source(doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) = {
    val title = ~doc.getText("doc.title")
    layout(
      title = title,
      active = "source",
      moreCss = frag(cssTag("source")),
      contentCls = "page"
    )(frag(
        div(cls := "box box-pad body")(
          h1(title),
          raw(~doc.getHtml("doc.content", resolver))
        ),
        br,
        div(cls := "box")(freeJs())
      ))
  }

  def webmasters()(implicit ctx: Context) = {
    val baseUrl = "https://lichess.org"
    val parameters = frag(
      p("Parameters:"),
      ul(
        li(strong("theme"), ": ", lila.pref.Theme.all.map(_.name).mkString(", ")),
        li(strong("bg"), ": light, dark")
      )
    )
    layout(
      title = "Webmasters",
      active = "webmasters",
      moreCss = cssTag("page"),
      contentCls = "page"
    )(frag(
        div(cls := "box box-pad developers body") {
          val args = """style="width: 400px; height: 444px;" allowtransparency="true" frameborder="0""""
          frag(
            h1(id := "embed-tv")("Embed Lichess TV in your site"),
            div(cls := "center")(raw(s"""<iframe src="/tv/frame?theme=wood" $args></iframe>""")),
            p("Add the following HTML to your site:"),
            p(cls := "copy-zone")(
              input(
                id := "tv-embed-src",
                cls := "copyable autoselect",
                value := s"""<iframe src="$baseUrl/tv/frame?theme=wood&bg=light" $args></iframe>"""
              ),
              button(title := "Copy code", cls := "copy button", dataRel := "tv-embed-src", dataIcon := "\"")
            ),
            parameters
          )
        },
        br,
        div(cls := "box box-pad developers body") {
          val args = """style="width: 400px; height: 444px;" allowtransparency="true" frameborder="0""""
          frag(
            h1(id := "embed-puzzle")("Embed the daily puzzle in your site"),
            div(cls := "center")(raw(s"""<iframe src="/training/frame?theme=wood" $args></iframe>""")),
            p("Add the following HTML to your site:"),
            p(cls := "copy-zone")(
              input(
                id := "puzzle-embed-src",
                cls := "copyable autoselect",
                value := s"""<iframe src="$baseUrl/training/frame?theme=wood&bg=light" $args></iframe>"""
              ),
              button(title := "Copy code", cls := "copy button", dataRel := "puzzle-embed-src", dataIcon := "\"")
            ),
            parameters,
            p("The text is automatically translated to your visitor's language.")
          )
        },
        br,
        div(cls := "box box-pad developers body") {
          val args = """style="width: 600px; height: 397px;" frameborder="0""""
          frag(
            h1("Embed a chess analysis in your site"),
            raw(s"""<iframe src="/study/embed/XtFCFYlM/GCUTf2Jk?bg=auto&theme=auto" $args></iframe>"""),
            p("Create ", a(href := routes.Study.allDefault(1))("a study"), ", then click the share button to get the HTML code for the current chapter."),
            parameters,
            p("The text is automatically translated to your visitor's language.")
          )
        },
        br,
        div(cls := "box box-pad developers body") {
          val args = """style="width: 600px; height: 397px;" frameborder="0""""
          frag(
            h1("Embed a chess game in your site"),
            raw(s"""<iframe src="/embed/MPJcy1JW?bg=auto&theme=auto" $args></iframe>"""),
            p(raw("""On a game analysis page, click the <em>"FEN &amp; PGN"</em> tab at the bottom, then """), "\"", em(trans.embedInYourWebsite(), "\".")),
            parameters,
            p("The text is automatically translated to your visitor's language.")
          )
        },
        br,
        div(cls := "box box-pad developers body")(
          h1("HTTP API"),
          p(raw("""Lichess exposes a RESTish HTTP/JSON API that you are welcome to use. Read the <a href="/api" class="blue">HTTP API documentation</a>."""))
        ),
        br,
        div(cls := "box box-pad developers body")(
          h1(id := "widgets")("Lichess Widgets"),
          p("Let your website/blog visitors know that you're playing on lichess!"),
          p(raw("""See <a href="https://rubenwardy.com/lichess_widgets/" class="blue">https://rubenwardy.com/lichess_widgets/</a> for widgets with your username and rating."""))
        )
      ))
  }

  def layout(
    title: String,
    active: String,
    contentCls: String = "",
    moreCss: Frag = emptyFrag,
    moreJs: Frag = emptyFrag
  )(body: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = moreCss,
    moreJs = moreJs
  ) {
    val sep = div(cls := "sep")
    val external = frag(" ", i(dataIcon := "0"))
    def activeCls(c: String) = cls := active.activeO(c)
    main(cls := "page-menu")(
      st.nav(cls := "page-menu__menu subnav")(
        a(activeCls("about"), href := routes.Page.about)(trans.aboutX("lichess.org")),
        a(activeCls("faq"), href := routes.Main.faq)("FAQ"),
        a(activeCls("contact"), href := routes.Main.contact)(trans.contact()),
        a(activeCls("tos"), href := routes.Page.tos)(trans.termsOfService()),
        a(activeCls("privacy"), href := routes.Page.privacy)(trans.privacy()),
        a(activeCls("master"), href := routes.Page.master)("Title verification"),
        sep,
        a(activeCls("source"), href := routes.Page.source)(trans.sourceCode()),
        a(activeCls("help"), href := routes.Page.help)(trans.contribute()),
        a(activeCls("thanks"), href := routes.Page.thanks)(trans.thankYou()),
        sep,
        a(activeCls("webmasters"), href := routes.Main.webmasters)(trans.webmasters()),
        a(activeCls("database"), href := "https://database.lichess.org")(trans.database(), external),
        a(activeCls("api"), href := routes.Api.index)("API", external),
        sep,
        a(activeCls("lag"), href := routes.Main.lag)("Is Lichess lagging?")
      ),
      div(cls := s"page-menu__content $contentCls")(body)
    )
  }
}
