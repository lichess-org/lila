package views.html.swiss

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.swiss.Swiss

import controllers.routes

object home {

  def apply(
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "Swiss tournaments",
      moreCss = cssTag("swiss.home")
    ) {
      main(cls := "page-small box box-pad page swiss-home")(
        h1("Swiss tournaments [BETA]"),
        div(cls := "swiss-home__infos")(
          div(cls := "wiki")(
            iconTag(""),
            p(
              "In a Swiss tournament, each competitor does not necessarily play all other entrants. Competitors meet one-on-one in each round and are paired using a set of rules designed to ensure that each competitor plays opponents with a similar running score, but not the same opponent more than once. The winner is the competitor with the highest aggregate points earned in all rounds. All competitors play in each round unless there is an odd number of players."
            )
          ),
          div(cls := "team")(
            iconTag("f"),
            p(
              "Swiss tournaments can only be created by team leaders, and can only be played by team members.",
              br,
              a(href := routes.Team.home())("Join or create a team"),
              " to start playing in swiss tournaments."
            )
          )
        )
      )
    }
}
