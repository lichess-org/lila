package views.html.site

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object help {

  def page(active: String, doc: io.prismic.Document, resolver: io.prismic.DocumentLinkResolver)(implicit ctx: Context) = {
    val title = ~doc.getText("doc.title")
    layout(
      title = title,
      active = active,
      moreCss = responsiveCssTag("page")
    )(main(cls := "page box box-pad")(
        h1(title),
        div(cls := "body")(raw(~doc.getHtml("doc.content", resolver)))
      ))
  }

  def webmasters()(implicit ctx: Context) = {
    val parameters = frag(
      p("Parameters:"),
      ul(
        li(strong("theme"), ": ", lidraughts.pref.Theme.all.map(_.name).mkString(", ")),
        li(strong("bg"), ": light, dark")
      )
    )
    layout(
      title = "Webmasters",
      active = "webmasters"
    )(frag(
      div(cls := "box box-pad developers")(
        h1(id := "embed-tv")("Embed Lidraughts TV in your site"),
        raw(s"""<script src="$netBaseUrl/tv/embed?theme=wood&bg=light"></script>"""),
        p("Just add the following HTML to your site:"),
        pre(s"""<script src="$netBaseUrl/tv/embed?theme=auto&bg=auto"></script>"""),
        parameters
      ),
      br,
      div(cls := "box box-pad developers")(
        h1(id := "embed-puzzle")("Embed the daily puzzle in your site"),
        raw("""<script src="/training/embed?theme=auto&bg=auto"></script>"""),
        p("Just add the following HTML to your site:"),
        pre(s"""<script src="$netBaseUrl/training/embed?theme=auto&bg=auto"></script>"""),
        parameters,
        p("The text is automatically translated to your visitor's language.")
      ),
      br,
      div(cls := "box box-pad developers")(
        h1("Embed a draughts analysis in your site"),
        raw(s"""<iframe width=530 height=353 src="$netBaseUrl/study/embed/xGDc4tlJ/AqJhrQbk?bg=auto&theme=auto" frameborder=0 style="margin-bottom: 1em"></iframe>"""),
        p("Create ", a(href := routes.Study.allDefault(1), cls := "blue")("a study"), ", then click the share button to get the HTML code for the current chapter."),
        pre(s"""<iframe width=600 height=397 frameborder=0
src="$netBaseUrl/study/embed/xGDc4tlJ/AqJhrQbk?bg=auto&theme=auto"
></iframe>"""),
        parameters,
        p("The text is automatically translated to your visitor's language.")
      ),
      br,
      div(cls := "box box-pad developers")(
        h1("Embed an interactive lesson in your site"),
        raw(s"""<iframe width=530 height=353 src="$netBaseUrl/study/embed/vxL8cJ67/fh6Ycb8X?next=true&bg=auto&theme=auto" frameborder=0 style="margin-bottom: 1em"></iframe>"""),
        p("Create ", a(href := routes.Study.allDefault(1), cls := "blue")("a study"), " with a chapter of type \"Interactive lesson\", then click the share button to get the HTML code for that chapter."),
        pre(s"""<iframe width=600 height=397 frameborder=0
src="$netBaseUrl/study/embed/vxL8cJ67/fh6Ycb8X?next=true&bg=auto&theme=auto"
></iframe>"""),
        parameters,
        p("The text is automatically translated to your visitor's language.")
      ),
      br,
      div(cls := "box box-pad developers")(
        h1("Embed a draughts game in your site"),
        raw(s"""<iframe width=530 height=353 src="$netBaseUrl/embed/JLuuVBv5?bg=auto&theme=auto" frameborder=0 style="margin-bottom: 1em"></iframe>"""),
        p(raw("""On a game analysis page, click the <em>"FEN &amp; PGN"</em> tab at the bottom, then """), "\"", em(trans.embedInYourWebsite.frag(), "\".")),
        pre(s"""<iframe width="600" height="397" frameborder="0"
src="$netBaseUrl/embed/JLuuVBv5?theme=auto&bg=auto"
></iframe>"""),
        parameters,
        p("The text is automatically translated to your visitor's language.")
      )
    /*br,
      div(cls := "box box-pad developers")(
        h1("HTTP API"),
        p(raw("""Lidraughts exposes a RESTish HTTP/JSON API that you are welcome to use. Read the <a href="/api" class="blue">HTTP API documentation</a>."""))
      )*/
    ))
  }

  def layout(
    title: String,
    active: String,
    moreCss: Frag = emptyFrag,
    moreJs: Frag = emptyFrag
  )(body: Frag)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = moreCss,
    moreJs = moreJs,
    responsive = true
  ) {
    val sep = div(cls := "sep")
    val external = frag(" ", i(dataIcon := "0"))
    def activeCls(c: String) = cls := active.activeO(c)
    main(cls := "page-menu")(
      st.nav(cls := "page-menu__menu subnav")(
        a(activeCls("about"), href := routes.Page.about)(trans.aboutX("lidraughts.org")),
        a(href := routes.QaQuestion.index(None))(trans.questionsAndAnswers()),
        a(activeCls("master"), href := routes.Page.master)("Title verification"),
        a(activeCls("contact"), href := routes.Page.contact)(trans.contact()),
        sep,
        a(activeCls("tos"), href := routes.Page.tos)(trans.termsOfService()),
        a(activeCls("privacy"), href := routes.Page.privacy)(trans.privacy()),
        sep,
        a(activeCls("webmasters"), href := routes.Main.webmasters)(trans.webmasters()),
        /*a(activeCls("database"), href := "https://database.lichess.org")(trans.database(), external),
        a(activeCls("api"), href := "https://database.lichess.org")("API", external),*/
        a(activeCls("source"), href := "https://github.com/RoepStoep/lidraughts")(trans.sourceCode(), external),
        sep,
        a(activeCls("lag"), href := routes.Main.lag)("Is Lidraughts lagging?")
      ),
      div(cls := "page-menu__content")(body)
    )
  }
}
