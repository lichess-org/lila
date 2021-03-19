package views
package html.plan

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object features {

  def apply()(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = cssTag("feature"),
      openGraph = lila.app.ui
        .OpenGraph(
          title = title,
          url = s"$netBaseUrl${routes.Plan.features.url}",
          description = "All of Lichess features are free for all and forever. We do it for the chess!"
        )
        .some
    ) {
      main(cls := "box box-pad features")(
        table(
          header(h1(dataIcon := "")("Website")),
          tbody(
            tr(unlimited)(
              "Play and create ",
              a(href := routes.Tournament.home)("tournaments")
            ),
            tr(unlimited)(
              "Play and create ",
              a(href := routes.Simul.home)("simultaneous exhibitions")
            ),
            tr(unlimited)(
              "Correspondence chess with conditional premoves"
            ),
            tr(check)(
              "Standard chess and ",
              a(href := routes.Page.variantHome)("8 chess variants (Crazyhouse, Chess960, Horde, ...)")
            ),
            tr(custom("30 per day"))(
              "Deep Stockfish 13+ server analysis"
            ),
            tr(unlimited)(
              "Instant local Stockfish 13+ analysis"
            ),
            tr(unlimited)(
              a(href := "https://lichess.org/blog/WN-gLzAAAKlI89Xn/thousands-of-stockfish-analysers")(
                "Cloud engine analysis"
              )
            ),
            tr(unlimited)(
              a(href := "https://lichess.org/blog/WFvLpiQAACMA8e9D/learn-from-your-mistakes")(
                "Learn from your mistakes"
              )
            ),
            tr(unlimited)(
              a(href := "https://lichess.org/blog/V0KrLSkAAMo3hsi4/study-chess-the-lichess-way")(
                "Studies (shared and persistent analysis)"
              )
            ),
            tr(unlimited)(
              a(href := "https://lichess.org/blog/VmZbaigAABACtXQC/chess-insights")(
                "Chess insights (detailed analysis of your play)"
              )
            ),
            tr(check)(
              a(href := routes.Learn.index)("All chess basics lessons")
            ),
            tr(unlimited)(
              a(href := routes.Puzzle.home)("Tactical puzzles from user games")
            ),
            tr(unlimited)(
              a(href := routes.Storm.home)("Puzzle Storm")
            ),
            tr(unlimited)(
              a(href := routes.Racer.home)("Puzzle Racer")
            ),
            tr(unlimited)(
              a(href := s"${routes.UserAnalysis.index}#explorer")("Opening explorer"),
              " (280 million games!)"
            ),
            tr(unlimited)(
              a(href := s"${routes.UserAnalysis.parseArg("QN4n1/6r1/3k4/8/b2K4/8/8/8_b_-_-")}#explorer")(
                "7-piece endgame tablebase"
              )
            ),
            tr(check)(
              "Download/Upload any game as PGN"
            ),
            tr(unlimited)(
              a(href := routes.Search.index(1))("Advanced search"),
              " through Lichess 3 billion games"
            ),
            tr(unlimited)(
              a(href := routes.Video.index)("Chess video library")
            ),
            tr(check)(
              "Forum, teams, messaging, friends, challenges"
            ),
            tr(check)(
              "Available in ",
              a(href := "https://crowdin.com/project/lichess")("80+ languages")
            ),
            tr(check)(
              "Light/dark theme, custom boards, pieces and background"
            ),
            tr(check)(
              strong("Zero ads")
            ),
            tr(check)(
              strong("No tracking")
            ),
            tr(check)(
              strong("All features to come, forever")
            )
          ),
          header(h1(dataIcon := "")("Mobile")),
          tbody(
            tr(unlimited)(
              "Online and offline games, with 8 variants"
            ),
            tr(unlimited)(
              "Bullet, Blitz, Rapid, Classical and Correspondence chess"
            ),
            tr(unlimited)(
              a(href := routes.Tournament.home)("Arena tournaments")
            ),
            tr(check)(
              "Board editor and analysis board with Stockfish 12+"
            ),
            tr(unlimited)(
              a(href := routes.Puzzle.home)("Tactics puzzles")
            ),
            tr(check)(
              "Available in 80+ languages"
            ),
            tr(check)(
              "Light and dark theme, custom boards and pieces"
            ),
            tr(check)(
              "iPhone & Android phones and tablets, landscape support"
            ),
            tr(check)(
              strong("Zero ads, no tracking")
            ),
            tr(check)(
              strong("All features to come, forever")
            )
          ),
          header(h1("Support Lichess")),
          tbody(cls := "support")(
            st.tr(
              th(
                "Contribute to Lichess and",
                br,
                "get a cool looking Patron icon"
              ),
              td("-"),
              td(span(dataIcon := patronIconChar, cls := "is is-green text check")("Yes"))
            ),
            st.tr(cls := "price")(
              th,
              td(cls := "green")("$0"),
              td(a(href := routes.Plan.index, cls := "green button")("$5/month"))
            )
          )
        ),
        p(cls := "explanation")(
          strong("Yes, both accounts have the same features!"),
          br,
          "That is because Lichess is built for the love of chess.",
          br,
          "We believe every chess player deserves the best, and so:",
          br,
          br,
          strong("all features are free for everybody, forever!"),
          br,
          "If you love Lichess, ",
          a(cls := "button", href := routes.Plan.index)("Support us with a Patron account!")
        )
      )
    }

  private def header(name: Frag)(implicit lang: Lang) =
    thead(
      st.tr(th(name), th(trans.patron.freeAccount()), th(trans.patron.lichessPatron()))
    )

  private val unlimited = span(dataIcon := "E", cls := "is is-green text unlimited")("Unlimited")

  private val check = span(dataIcon := "E", cls := "is is-green text check")("Yes")

  private def custom(str: String) = span(dataIcon := "E", cls := "is is-green text check")(str)

  private def all(content: Frag) = frag(td(content), td(content))

  private def tr(value: Frag)(text: Frag*) = st.tr(th(text), all(value))

  private val title = "Lichess features"
}
