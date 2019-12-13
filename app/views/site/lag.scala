package views.html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object lag {

  def apply()(implicit ctx: Context) =
    help.layout(
      title = "Is Lichess lagging?",
      active = "lag",
      moreCss = cssTag("lag"),
      moreJs = frag(
        highchartsLatestTag,
        highchartsMoreTag,
        jsTag("lag.js")
      )
    ) {
      main(cls := "box box-pad lag")(
        h1(
          "Is Lichess lagging?",
          span(cls := "answer short")(
            span(cls := "waiting")("Measurements in progress..."),
            span(cls := "nope-nope none")(strong("No."), " And your network is good."),
            span(cls := "nope-yep none")(strong("No."), " But your network is bad."),
            span(cls := "yep none")(strong("Yes."), " It will be fixed soon!")
          )
        ),
        div(cls := "answer long")(
          "And now, the long answer! Game lag is composed of two unrelated values (lower is better):"
        ),
        div(cls := "sections")(
          st.section(cls := "server")(
            h2("Lichess server latency"),
            div(cls := "meter"),
            p(
              "The time it takes to process a move on the server. ",
              "It's the ",
              strong("same for everybody"),
              ", and only depends on the server load. ",
              "The more players and the higher it gets, but Lichess developers ",
              "do their best to keep it low. It rarely exceeds 10ms."
            )
          ),
          st.section(cls := "network")(
            h2("Network between Lichess and you"),
            div(cls := "meter"),
            p(
              "The time it takes to send a move from your computer to Lichess server, ",
              "and get the response back. ",
              "It's specific to your ",
              strong("distance to Lichess (France)"),
              ", and ",
              "to the ",
              strong("quality of your Internet connection"),
              ". ",
              "Lichess developers can not fix your wifi or make light go faster."
            )
          )
        ),
        div(cls := "last-word")(
          p("You can find both these values at any time, by clicking your username in the top bar."),
          h2("Lag compensation"),
          p(
            "Lichess compensates network lag. This includes sustained lag and occasional lag spikes. ",
            "There are limits and heuristics based on time control and the compensated lag so far, ",
            "so that the result should feel reasonable for both players. ",
            "As a result, having a higher network lag than your opponent is ",
            strong("not a handicap"),
            "!"
          )
        )
      )
    }
}
