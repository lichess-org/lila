package views
package html.plan

import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object features:

  val engineFullName = "Stockfish 16 NNUE"

  def apply()(using PageContext) =
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
          header(h1(dataIcon := licon.ScreenDesktop)("Website")),
          tbody(
            tr(check)(
              strong("Zero ads")
            ),
            tr(check)(
              strong("No tracking")
            ),
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
              a(href := routes.ContentPage.variantHome)("8 chess variants (Crazyhouse, Chess960, Horde, ...)")
            ),
            tr(custom(s"${lila.fishnet.FishnetLimiter.maxPerDay} per day"))(
              "Deep ",
              engineFullName,
              " server analysis"
            ),
            tr(unlimited)(
              "Instant local Stockfish 14+ analysis (depth 99)"
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
              a(href := routes.Puzzle.streak)("Puzzle Streak"),
              ", ",
              a(href := routes.Storm.home)("Puzzle Storm"),
              ", ",
              a(href := routes.Racer.home)("Puzzle Racer")
            ),
            tr(check)(
              a(href := s"${routes.UserAnalysis.index}#explorer")("Global opening explorer"),
              " (430 million games!)"
            ),
            tr(check)(
              a(href := s"${routes.UserAnalysis.index}#explorer/me")("Personal opening explorer"),
              " (also works on ",
              a(href := s"${routes.UserAnalysis.index}#explorer/DrNykterstein")("other players"),
              ")"
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
              " through Lichess 4 billion games"
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
              strong("All features to come, forever")
            )
          ),
          header(h1(dataIcon := licon.PhoneMobile)("Mobile")),
          tbody(
            tr(check)(
              strong("Zero ads, no tracking")
            ),
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
              "Board editor and analysis board with Stockfish 14+"
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

  private def header(name: Frag)(using Lang) =
    thead(
      st.tr(th(name), th(trans.patron.freeAccount()), th(trans.patron.lichessPatron()))
    )

  private val unlimited = span(dataIcon := licon.Checkmark, cls := "is is-green text unlimited")("Unlimited")

  private val check = span(dataIcon := licon.Checkmark, cls := "is is-green text check")("Yes")

  private def custom(str: String) = span(dataIcon := licon.Checkmark, cls := "is is-green text check")(str)

  private def all(content: Frag) = frag(td(content), td(content))

  private def tr(value: Frag)(text: Frag*) = st.tr(th(text), all(value))

  private val title = "Lichess features"
