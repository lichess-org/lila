package lila.web
package views

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class SitePages(helpers: Helpers):
  import helpers.{ *, given }

  def menu(active: String)(using Translate) =
    val sep                  = div(cls := "sep")
    val external             = frag(" ", i(dataIcon := Icon.ExternalArrow))
    def activeCls(c: String) = cls := active.activeO(c)
    lila.ui.bits.pageMenuSubnav(
      a(activeCls("about"), href := "/about")(trans.site.aboutX("lichess.org")),
      a(activeCls("news"), href := routes.Feed.index(1))("Lichess updates"),
      a(activeCls("faq"), href := routes.Main.faq)(trans.faq.faqAbbreviation()),
      a(activeCls("contact"), href := routes.Main.contact)(trans.contact.contact()),
      a(activeCls("tos"), href := routes.Cms.tos)(trans.site.termsOfService()),
      a(activeCls("privacy"), href := "/privacy")(trans.site.privacy()),
      a(activeCls("master"), href := routes.Cms.master)("Title verification"),
      sep,
      a(activeCls("source"), href := routes.Cms.source)(trans.site.sourceCode()),
      a(activeCls("help"), href := routes.Cms.help)(trans.site.contribute()),
      a(activeCls("changelog"), href := routes.Cms.menuPage("changelog"))("Changelog"),
      a(activeCls("thanks"), href := "/thanks")(trans.site.thankYou()),
      sep,
      a(activeCls("webmasters"), href := routes.Main.webmasters)(trans.site.webmasters()),
      a(activeCls("database"), href := "https://database.lichess.org")(trans.site.database(), external),
      a(activeCls("api"), href := routes.Api.index)("API", external),
      sep,
      a(activeCls("lag"), href := routes.Main.lag)(trans.lag.isLichessLagging()),
      a(activeCls("ads"), href := "/ads")("Block ads")
    )

  def webmasters(params: Modifier*)(using Translate) =
    val parameters = frag(p("Parameters:"), ul(params, li(strong("bg"), ": light, dark, system")))
    frag(
      st.section(cls := "box box-pad developers")(
        h1(cls := "box__top")("HTTP API"),
        p(
          "Lichess exposes a RESTish HTTP/JSON API that you are welcome to use. Read the ",
          a(href := "/api")("HTTP API documentation"),
          "."
        )
      ),
      br,
      st.section(cls := "box box-pad developers") {
        val args = """style="width: 400px; height: 444px;" allowtransparency="true" frameborder="0""""
        frag(
          h1(cls := "box__top", id := "embed-tv")("Embed Lichess TV in your site"),
          div(cls := "body")(
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
                dataIcon := Icon.Link
              )
            ),
            parameters,
            p(
              "You can also show the channel for a specific variant or time control by adding the channel key to the URL, corresponding to the channels available at ",
              a(href := "/tv")("lichess.org/tv"),
              ". If not included, the top rated game will be shown."
            ),
            p(cls := "copy-zone")(
              input(
                id    := "tv-channel-embed-src",
                cls   := "copyable autoselect",
                value := s"""<iframe src="$netBaseUrl/tv/rapid/frame?theme=brown&bg=dark" $args></iframe>"""
              ),
              button(
                st.title := "Copy code",
                cls      := "copy button",
                dataRel  := "tv-channel-embed-src",
                dataIcon := Icon.Link
              )
            )
          )
        )
      },
      br,
      st.section(cls := "box box-pad developers") {
        val args = """style="width: 400px; height: 444px;" allowtransparency="true" frameborder="0""""
        frag(
          h1(cls := "box__top", id := "embed-puzzle")("Embed the daily puzzle in your site"),
          div(cls := "body")(
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
                dataIcon := Icon.Link
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
        )
      },
      br,
      st.section(cls := "box box-pad developers") {
        val args = """style="width: 600px; height: 397px;" frameborder="0""""
        frag(
          h1(cls := "box__top", id := "embed-study")("Embed a chess analysis in your site"),
          div(cls := "body")(
            div(cls := "center"):
              raw(s"""<iframe src="/study/embed/XtFCFYlM/GCUTf2Jk?bg=auto&theme=auto" $args></iframe>""")
            ,
            p(
              "Create ",
              a(href := routes.Study.allDefault())("a study"),
              ", then click the share button to get the HTML code for the current chapter."
            ),
            parameters,
            p("The text is automatically translated to your visitor's language.")
          )
        )
      },
      br,
      st.section(cls := "box box-pad developers") {
        val args = """style="width: 600px; height: 397px;" frameborder="0""""
        frag(
          h1(cls := "box__top")("Embed a chess game in your site"),
          div(cls := "body")(
            div(cls := "center"):
              raw(s"""<iframe src="/embed/game/MPJcy1JW?bg=auto&theme=auto" $args></iframe>""")
            ,
            p(
              raw(
                """On a game analysis page, click the <em>"FEN &amp; PGN"</em> tab at the bottom, then """
              ),
              "\"",
              em(trans.site.embedInYourWebsite(), "\".")
            ),
            parameters,
            p("The text is automatically translated to your visitor's language.")
          )
        )
      }
    )
