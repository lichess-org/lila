package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.Tournament

import controllers.routes

object finishedPaginator {

  def apply(finished: lila.common.paginator.Paginator[Tournament])(implicit ctx: Context) =
    tbody(cls := "infinitescroll")(
      finished.nextPage.map { np =>
        tr(th(cls := "pager none")(
          a(rel := "next", href := routes.Tournament.home(np))("Next")
        ))
      },
      finished.currentPageResults.map { t =>
        tr(cls := List(
          "paginated" -> true,
          "tour-scheduled" -> t.isScheduled
        ))(
          td(cls := "icon")(iconTag(tournamentIconChar(t))),
          td(cls := "header")(
            a(href := routes.Tournament.show(t.id))(
              span(cls := "name")(t.fullName),
              span(cls := "setup")(
                t.clock.show,
                " • ",
                if (t.variant.exotic) t.variant.name else t.perfType.map(_.name),
                !t.position.initial option frag(" • ", trans.thematic()),
                " • ",
                t.mode.fold(trans.casualTournament, trans.ratedTournament)()
              )
            )
          ),
          td(cls := "duration")(t.durationString),
          td(cls := "winner")(
            userIdLink(t.winnerId, withOnline = false),
            br
          ),
          td(cls := "text", dataIcon := "r")(t.nbPlayers.localize)
        )
      }
    )
}
