package views
package html.tournament

import lidraughts.tournament.Tournament
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object enterable {

  def apply(tours: List[Tournament]) =
    table(cls := "tournaments")(
      tours map { tour =>
        tr(
          td(cls := "name")(
            a(cls := "text", href := routes.Tournament.show(tour.id))(
              span(dataIcon := tournamentIconChar(tour)),
              tour.name
            )
          ),
          tour.schedule.fold(td()) { s => td(momentFromNow(s.at)) },
          td(tour.durationString),
          td(dataIcon := "r", cls := "text")(tour.nbPlayers)
        )
      }
    )
}
