package views.html.site

import controllers.routes
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

object lag {

  def apply()(implicit ctx: Context) = help.layout(
    title = "Is Lidraughts lagging?",
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
          "Is Lidraughts lagging?",
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
            h2("Lidraughts server latency"),
            div(cls := "meter"),
            p(
              "The time it takes to process a move on the server. ",
              "It's the ", strong("same for everybody"), ", and only depends on the server load. ",
              "The more players and the higher it gets, but Lidraughts developers ",
              "do their best to keep it low. It rarely exceeds 10ms."
            )
          ),
          st.section(cls := "network")(
            h2("Network between Lidraughts and you"),
            div(cls := "meter"),
            p(
              "The time it takes to send a move from your computer to Lidraughts server, ",
              "and get the response back.",
              "It's specific to your ", strong("distance to Lidraughts (France)"), ", and ",
              "to the ", strong("quality of your Internet connection"), ". ",
              "Lidraughts developers can not fix your wifi or make light go faster."
            )
          )
        ),
        div(cls := "last-word")(
          p("You can find both these values at any time, by clicking your username in the top bar."),
          h2("Lag compensation"),
          p(
            "Lidraughts compensates network lag, up to one second per move. ",
            "After your move, ", strong("your average network lag is added to your clock"), ". ",
            "As a result, having a higher network lag than your opponent is ", strong("not a handicap"), "!"
          )
        )
      )
    }
}
