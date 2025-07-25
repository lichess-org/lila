package lila.web
package ui

import lila.core.id.{ CmsPageKey, ForumCategId }
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SitePages(helpers: Helpers):
  import helpers.{ *, given }

  def SitePage(title: String, active: String, contentCls: String = "")(using Context): Page =
    Page(title).wrap: body =>
      main(cls := "page-menu")(
        menu(active),
        div(cls := s"page-menu__content $contentCls")(body)
      )

  def menu(active: String)(using Translate) =
    val sep = div(cls := "sep")
    val external = frag(" ", i(dataIcon := Icon.ExternalArrow))
    def activeCls(c: String) = cls := active.activeO(c)
    lila.ui.bits.pageMenuSubnav(
      a(activeCls("about"), href := "/about")(trans.site.aboutX("lichess.org")),
      a(activeCls("news"), href := routes.Feed.index(1))("Lichess updates"),
      a(activeCls("faq"), href := routes.Main.faq)(trans.faq.faqAbbreviation()),
      a(activeCls("contact"), href := routes.Main.contact)(trans.contact.contact()),
      a(activeCls("tos"), href := routes.Cms.tos)(trans.site.termsOfService()),
      a(activeCls("privacy"), href := "/privacy")(trans.site.privacy()),
      a(activeCls("title"), href := routes.TitleVerify.index)(trans.site.titleVerification()),
      sep,
      a(activeCls("source"), href := routes.Cms.source)(trans.site.sourceCode()),
      a(activeCls("help"), href := routes.Cms.help)(trans.site.contribute()),
      a(activeCls("changelog"), href := routes.Cms.menuPage(CmsPageKey("changelog")))("Changelog"),
      a(activeCls("thanks"), href := "/thanks")(trans.site.thankYou()),
      sep,
      a(activeCls("webmasters"), href := routes.Main.webmasters)(trans.site.webmasters()),
      a(activeCls("database"), href := "https://database.lichess.org")(trans.site.database(), external),
      a(activeCls("api"), href := routes.Api.index)("API", external),
      sep,
      a(activeCls("lag"), href := routes.Main.lag)(trans.lag.isLichessLagging()),
      a(activeCls("ads"), href := "/ads")("Block ads")
    )

  def webmasters(pieceNames: List[String])(using Context) =
    def parameters(extra: Modifier*) = frag(
      p("Parameters:"),
      ul(
        // actual supported board theme list from lila-gif/src/assets.rs
        li(strong("theme"), ": ", List("blue", "brown", "green", "ic", "purple").mkString(", ")),
        li(strong("pieceSet"), ": ", pieceNames.mkString(", ")),
        li(strong("bg"), ": light, dark, system"),
        extra
      )
    )
    SitePage(
      title = "Webmasters",
      active = "webmasters",
      contentCls = "page force-ltr"
    ).css("bits.page")
      .csp(_.copy(frameSrc = "https://lichess.org" :: Nil)):
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
            val args =
              """style="width: 400px; aspect-ratio: 10/11;" allowtransparency="true" frameborder="0""""
            frag(
              h1(cls := "box__top", id := "embed-tv")("Embed Lichess TV in your site"),
              div(cls := "body")(
                div(cls := "center")(raw(s"""<iframe src="/tv/frame?theme=brown&bg=dark" $args></iframe>""")),
                p("Add the following HTML to your site:"),
                copyMeInput(s"""<iframe src="$netBaseUrl/tv/frame?theme=brown&bg=dark" $args></iframe>"""),
                parameters(),
                p(
                  "You can also show the channel for a specific variant or time control by adding the channel key to the URL, corresponding to the channels available at ",
                  a(href := "/tv")("lichess.org/tv"),
                  ". If not included, the top rated game will be shown."
                ),
                copyMeInput(
                  s"""<iframe src="$netBaseUrl/tv/rapid/frame?theme=brown&bg=dark" $args></iframe>"""
                )
              )
            )
          },
          br,
          st.section(cls := "box box-pad developers") {
            val args =
              """style="width: 400px; aspect-ratio: 10/11;" allowtransparency="true" frameborder="0""""
            frag(
              h1(cls := "box__top", id := "embed-puzzle")("Embed the daily puzzle in your site"),
              div(cls := "body")(
                div(cls := "center")(
                  raw(s"""<iframe src="/training/frame?theme=brown&bg=dark" $args></iframe>""")
                ),
                p("Add the following HTML to your site:"),
                copyMeInput(
                  s"""<iframe src="$netBaseUrl/training/frame?theme=brown&bg=dark" $args></iframe>"""
                ),
                parameters(),
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
            val args = """style="width: 100%; aspect-ratio: 3/2;" frameborder="0""""
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
                parameters(),
                p("The text is automatically translated to your visitor's language.")
              )
            )
          },
          br,
          st.section(cls := "box box-pad developers") {
            val args = """style="width: 100%; aspect-ratio: 3/2;" frameborder="0""""
            frag(
              h1(cls := "box__top")("Embed a chess game in your site"),
              div(cls := "body")(
                div(cls := "center"):
                  raw(s"""<iframe src="/embed/game/MPJcy1JW?bg=auto&theme=auto" $args></iframe>""")
                ,
                p(
                  "On a game analysis page, click the ",
                  em("FEN & PGN"),
                  " tab at the bottom, then ",
                  "\"",
                  em(trans.site.embedInYourWebsite(), "\".")
                ),
                parameters(),
                p("The text is automatically translated to your visitor's language.")
              )
            )
          },
          br,
          st.section(cls := "box box-pad developers", id := "broadcast") {
            val args = """style="width: 100%; aspect-ratio: 4/3;" frameborder="0""""
            frag(
              h1(cls := "box__top")("Embed a broadcast in your site"),
              div(cls := "body")(
                div(cls := "center"):
                  raw:
                    s"""<iframe src="https://lichess.org/embed/broadcast/fide-world-rapidblitz-team-championships-2024--rapid-matches-1-10/G1YjiG7j" $args></iframe>"""
                ,
                p(
                  "On a broadcast page, select the embed iframe code, then optionally add query parameters to customize the appearance."
                ),
                parameters(),
                p("The text is automatically translated to your visitor's language.")
              )
            )
          },
          br,
          st.section(cls := "box box-pad developers", id := "analysis") {
            val args = """style="width: 100%; aspect-ratio: 4/3;" frameborder="0""""
            val iframe =
              s"""<iframe src="https://lichess.org/embed/analysis" $args></iframe>"""
            frag(
              h1(cls := "box__top")("Embed an analysis board"),
              div(cls := "body")(
                div(cls := "center")(raw(iframe)),
                p(
                  "Embeds the ",
                  a(href := routes.UserAnalysis.index)("fully-featured Lichess analysis board"),
                  " with stockfish evaluation, opening explorer and tablebase."
                ),
                copyMeInput(iframe),
                parameters(
                  li(strong("fen"), ": custom initial position as a FEN with underscores instead of spaces"),
                  li(strong("color"), ": initial orientation, either black or white")
                ),
                div(
                  "Example using a custom initial position:",
                  copyMeInput:
                    s"""<iframe src="https://lichess.org/embed/analysis?fen=r1bqkb1r/pp2pppp/2np1n2/6B1/3NP3/2N5/PPP2PPP/R2QKB1R_b_KQkq_-_1_6&color=black" $args></iframe>"""
                ),
                p("The text is automatically translated to your visitor's language.")
              )
            )
          }
        )

  def source(title: String, rendered: Frag, version: Option[WebConfig.LilaVersion])(using
      Context
  ) =
    SitePage(title = title, active = "source", contentCls = "page force-ltr")
      .css("bits.source")
      .js(esmInitBit("setAssetInfo")):
        frag(
          st.section(cls := "box")(
            h1(cls := "box__top")(title),
            table(cls := "slist slist-pad", id := "version")(
              thead(
                tr(
                  th(colspan := 3)("Current versions"),
                  th(colspan := 2)("Last boot: ", momentFromNow(lila.common.Uptime.startedAt))
                )
              ),
              tbody(
                version.map: v =>
                  tr(
                    td("Server"),
                    td(v.date),
                    td:
                      a(href := s"https://github.com/lichess-org/lila/commits/${v.commit}"):
                        pre(v.commit.take(7))
                    ,
                    td(v.message),
                    td:
                      a(href := s"https://github.com/lichess-org/lila/compare/${v.commit}...master"):
                        pre("...")
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
          st.section(cls := "box box-pad body")(rendered)
        )

  def lag(using Context) =
    import trans.lag as trl
    SitePage(title = "Is Lichess lagging?", active = "lag")
      .css("bits.lag")
      .js(esmInit("chart.lag")):
        div(cls := "box box-pad lag")(
          h1(cls := "box__top")(
            trl.isLichessLagging(),
            span(cls := "answer short")(
              span(cls := "waiting")(trl.measurementInProgressThreeDot()),
              span(cls := "nope-nope none")(trl.noAndYourNetworkIsGood()),
              span(cls := "nope-yep none")(trl.noAndYourNetworkIsBad()),
              span(cls := "yep none")(trl.yesItWillBeFixedSoon())
            )
          ),
          div(cls := "answer long")(
            trl.andNowTheLongAnswerLagComposedOfTwoValues()
          ),
          div(cls := "sections")(
            st.section(cls := "server")(
              h2(trl.lichessServerLatency()),
              div(cls := "meter")(canvas(cls := "server-chart")),
              p(
                trl.lichessServerLatencyExplanation()
              )
            ),
            st.section(cls := "network")(
              h2(trl.networkBetweenLichessAndYou()),
              div(cls := "meter")(canvas(cls := "network-chart")),
              p(
                trl.networkBetweenLichessAndYouExplanation()
              )
            )
          ),
          div(cls := "last-word")(
            p(trl.youCanFindTheseValuesAtAnyTimeByClickingOnYourUsername()),
            h2(trl.lagCompensation()),
            p(trl.lagCompensationExplanation())
          )
        )

  def dailyPuzzleSlackApp =
    Page("Daily Chess Puzzle by Lichess (Slack App)")
      .css("bits.page"):
        main(cls := "page page-small box box-pad")(
          h1(cls := "box__top")("Daily Chess Puzzle by Lichess (Slack App)"),
          div(cls := "body")(
            p(
              "Spice up your Slack workspace with a daily chess puzzle from ",
              a(href := "/")("lichess.org"),
              "."
            ),
            a(
              href := "https://slack.com/oauth/v2/authorize?client_id=17688987239.964622027363&scope=commands,incoming-webhook"
            )(
              img(
                alt := "Add to Slack",
                heightA := 40,
                widthA := 139,
                src := assetUrl("images/add-to-slack.png")
              )
            ),
            h2("Summary"),
            p(
              "By default, the app will post the ",
              a(href := routes.Puzzle.daily)("daily chess puzzle"),
              " from Lichess to the channel in which it was installed every day (at the same time of day it was installed). Use the ",
              code("/puzzletime"),
              " command to change this setting, e.g. ",
              code("/puzzletime 14:45"),
              ". To post the daily puzzle on demand, use the ",
              code("/puzzle"),
              " command."
            ),
            h2("Commands"),
            ul(
              li(code("/puzzlehelp"), " - Displays helpful instructions"),
              li(
                code("/puzzletime HH:MM"),
                " - Sets the time of day the daily puzzle should be posted (per channel)"
              ),
              li(code("/puzzle"), " - Posts the daily puzzle")
            ),
            h2("Privacy Policy"),
            p(
              "The app only collects and stores information necessary to deliver the service, which is limited to OAuth authentication information, Slack workspace/channel identifiers and app configuration settings. No personal information is processed except for the username of users invoking slash commands. No personal information is stored."
            ),
            h2("Support and feedback"),
            p(
              // Contact email, because Slack requires a support channel without
              // mandatory registration.
              "Give us feedback or ask questions ",
              a(href := routes.ForumCateg.show(ForumCategId("lichess-feedback")))(
                "in the forum"
              ),
              ". The source code is available at ",
              a(href := "https://github.com/arex1337/lichess-daily-puzzle-slack-app")(
                "github.com/arex1337/lichess-daily-puzzle-slack-app"
              ),
              "."
            )
          )
        )

  def getFishnet =
    Page("fishnet API key request")
      .csp(_.withGoogleForm):
        main:
          iframe(
            src := "https://docs.google.com/forms/d/e/1FAIpQLSeGgDHgWGP0uobQknF92eCMXqebyNBTyzJoJqbeGjRezlbWOw/viewform?embedded=true",
            style := "width:100%;height:1400px",
            st.frameborder := 0,
            frame.credentialless
          )(spinner)

  def errorPage =
    Page("Internal server error"):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")("Something went wrong on this page"),
        p(
          "If the problem persists, please ",
          a(href := s"${routes.Main.contact}#help-error-page")("report the bug"),
          "."
        )
      )
