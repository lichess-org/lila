package views
package html.simul

import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._

import controllers.routes

object allCreated {

  def apply(simuls: List[lila.simul.Simul]) =
    table(cls := "tournaments")(
      simuls map { simul =>
        tr(
          td(cls := "name")(
            a(cls := "text", href := routes.Simul.show(simul.id))(
              simul.perfTypes map { pt =>
                span(dataIcon := pt.iconChar)
              },
              simul.fullName
            )
          ),
          td(userIdLink(simul.hostId.some)),
          td(cls := "text", dataIcon := "p")(simul.clock.config.show),
          td(cls := "text", dataIcon := "r")(simul.applicants.size),
          td(a(href := routes.Simul.show(simul.id), cls := "button", dataIcon := "G"))
        )
      }
    )
}
