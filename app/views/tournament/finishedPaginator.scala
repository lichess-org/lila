package views.html.tournament

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.tournament.Tournament

import controllers.routes

object finishedPaginator {

  def apply(finished: lidraughts.common.paginator.Paginator[Tournament])(implicit ctx: Context) =
    tbody(cls := "infinitescroll")(
      finished.nextPage.map { np =>
        tr(th(cls := "pager none")(
          a(rel := "next", href := routes.Tournament.home(np))(trans.next())
        ))
      },
      finished.currentPageResults.map { t =>
        tr(cls := List(
          "paginated" -> true,
          "tour-scheduled" -> t.isScheduled
        ))(
          td(cls := "icon")(iconTag(tournamentIconChar(t))),
          header(t),
          td(cls := "duration")(t.durationString),
          td(cls := "winner")(
            userIdLink(t.winnerId, withOnline = false),
            br
          ),
          td(cls := "text", dataIcon := "r")(t.nbPlayers.localize)
        )
      }
    )

  def header(t: Tournament)(implicit cts: Context) =
    td(cls := "header")(
      a(href := routes.Tournament.show(t.id))(
        span(cls := "name")(t.fullName),
        span(cls := "setup")(
          t.clock.show,
          " • ",
          if (t.variant.exotic) t.variant.name else t.perfType.map(_.name),
          t.isThematic option frag(" • ", trans.thematic()),
          " • ",
          t.mode.fold(trans.casualTournament, trans.ratedTournament)(),
          " • ",
          t.durationString
        )
      )
    )
}
