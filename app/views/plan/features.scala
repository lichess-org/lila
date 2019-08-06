package views
package html.plan

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object features {

  def apply()(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("feature"),
    openGraph = lidraughts.app.ui.OpenGraph(
      title = title,
      url = s"$netBaseUrl${routes.Plan.features.url}",
      description = "All of Lidraughts features are free for all and forever. We do it for draughts!"
    ).some
  ) {
      main(cls := "box box-pad features")(
        table(
          header(h1(dataIcon := "")("Website")),
          tbody(
            tr(unlimited)(
              "Play and create ",
              a(href := routes.Tournament.home(1))("tournaments")
            ),
            tr(unlimited)(
              "Play and create ",
              a(href := routes.Simul.home)("simultaneous exhibitions")
            ),
            tr(unlimited)(
              "Correspondence draughts with conditional premoves"
            ),
            tr(check)(
              "Standard draughts and ",
              a(href := routes.Page.variantHome)("the variants Frisian, Antidraughts, Breakthrough, Frysk!")
            ),
            tr(custom("30 per day"))(
              s"Deep $engineName server analysis"
            ),
            tr(unlimited)(
              s"Instant local $engineName analysis"
            ),
            tr(unlimited)(
              "Cloud engine analysis"
            ),
            tr(unlimited)(
              "Learn from your mistakes"
            ),
            tr(unlimited)(
              "Studies (shared and persistent analysis)"
            ),
            /*tr(unlimited)(
              "Draughts insights (detailed analysis of your play)"
            ),
            tr(check)(
              a(href := routes.Learn.index)("All draughts basics lessons")
            ),*/
            tr(unlimited)(
              a(href := routes.Puzzle.home)("Tactical puzzles")
            ),
            /*tr(unlimited)(
              a(href := s"${routes.UserAnalysis.index()}#explorer")("Opening explorer"),
              " (62 million games!)"
            ),
            tr(unlimited)(
              a(href := s"${routes.UserAnalysis.parse("QN4n1/6r1/3k4/8/b2K4/8/8/8_b_-_-")}#explorer")("7-piece endgame tablebase")
            ),*/
            tr(check)(
              "Download/Upload any game as PDN"
            ),
            tr(unlimited)(
              a(href := routes.Search.index(1))("Advanced search"),
              " through all Lidraughts games"
            ),
            /*tr(unlimited)(
              a(href := routes.Video.index)("Draughts video library")
            ),*/
            tr(check)(
              "Forum, teams, messaging, friends, challenges"
            ),
            tr(check)(
              "Available in 17 languages"
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
              "Online and offline games, with all variants"
            ),
            tr(unlimited)(
              "Bullet, Blitz, Rapid, Classical and Correspondence draughts"
            ),
            tr(unlimited)(
              a(href := routes.Tournament.home(1))("Arena tournaments")
            ),
            tr(check)(
              s"Board editor and analysis board with $engineName"
            ),
            tr(unlimited)(
              a(href := routes.Puzzle.home)("Tactics puzzles")
            ),
            tr(check)(
              "Available in 17 languages"
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
          header(h1("Support Lidraughts")),
          tbody(cls := "support")(
            st.tr(
              th(
                "Contribute to Lidraughts and",
                br,
                "get a cool looking Patron icon"
              ),
              td("-"),
              td(span(dataIcon := patronIconChar, cls := "is is-green text check")("Yes"))
            ),
            st.tr(cls := "price")(
              th,
              td(cls := "green")("€0"),
              td(a(href := routes.Plan.index, cls := "green button")("€5/month"))
            )
          )
        ),
        p(cls := "explanation")(
          strong("Yes, both accounts have the same features!"),
          br,
          "That is because Lidraughts is built for the love of draughts.",
          br,
          "We believe every draughts player deserves the best, and so:",
          br, br,
          strong("all features are free for everybody, forever!"),
          br,
          "If you love Lidraughts, ",
          a(cls := "button", href := routes.Plan.index)("Support us with a Patron account!")
        )
      )
    }

  private def header(name: Frag) = thead(
    st.tr(th(name), th("Free account"), th("Lidraughts Patron"))
  )

  private val unlimited = span(dataIcon := "E", cls := "is is-green text unlimited")("Unlimited")

  private val check = span(dataIcon := "E", cls := "is is-green text check")("Yes")

  private def custom(str: String) = span(dataIcon := "E", cls := "is is-green text check")(str)

  private def all(content: Frag) = frag(td(content), td(content))

  private def tr(value: Frag)(text: Frag*) = st.tr(th(text), all(value))

  private val title = "Lidraughts features"

  private val engineName = "Scan 3.1"
}
