package views.html.tournament

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.Tournament

object bits {

  def notFound()(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.tournamentNotFound.txt()
    ) {
      main(cls := "page-small box box-pad")(
        h1(trans.tournamentNotFound()),
        p(trans.tournamentDoesNotExist()),
        p(trans.tournamentMayHaveBeenCanceled()),
        br,
        br,
        a(href := routes.Tournament.home)(trans.returnToTournamentsHomepage())
      )
    }

  def enterable(tours: List[Tournament]) =
    table(cls := "tournaments")(
      tours map { tour =>
        tr(
          td(cls := "name")(
            a(cls := "text", dataIcon := tournamentIconChar(tour), href := routes.Tournament.show(tour.id))(
              tour.name
            )
          ),
          // td(tour.format.trans),
          tour.schedule.fold(td) { s =>
            td(momentFromNow(s.at))
          },
          td(dataIcon := "r", cls := "text")(tour.nbPlayers)
        )
      }
    )

  def userPrizeDisclaimer =
    div(cls := "tour__prize")(
      "This tournament is NOT organized by Lishogi.",
      br,
      "If it has prizes, Lishogi is NOT responsible for paying them."
    )

}
