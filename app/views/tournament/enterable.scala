package views
package html.tournament

import lila.tournament.Tournament
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object enterable {

  def apply(tours: List[Tournament]) =
    table(cls := "tournaments")(
      tours map { tour =>
        tr(
          td(cls := "name")(
            a(cls := "text", dataIcon := tournamentIconChar(tour), href := routes.Tournament.show(tour.id))(tour.name)
          ),
          tour.schedule.fold(td()) { s => td(momentFromNow(s.at)) },
          td(tour.durationString),
          td(dataIcon := "r", cls := "text")(tour.nbPlayers)
        )
      }
    )
}
