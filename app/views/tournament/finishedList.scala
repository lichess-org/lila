package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.Tournament

import controllers.routes

object finishedList {

  def apply(finished: List[Tournament])(implicit ctx: Context): Tag =
    tbody(finished map apply)

  def apply(t: Tournament)(implicit ctx: Context): Tag =
    tr(cls := "paginated")(
      td(cls := "icon")(iconTag(tournamentIconChar(t))),
      header(t),
      td(cls := "date")(momentFromNow(t.startsAt)),
      td(cls := "players")(
        span(
          iconTag('g')(cls := "text"),
          userIdLink(t.winnerId, withOnline = false)
        ),
        span(trans.nbPlayers.plural(t.nbPlayers, t.nbPlayers.localize))
      )
    )

  def header(t: Tournament)(implicit ctx: Context) =
    td(cls := "header")(
      a(href := routes.Tournament.show(t.id))(
        span(cls := "name")(t.name()),
        span(
          t.clock.show,
          " • ",
          if (t.variant.exotic) t.variant.name else t.perfType.trans,
          t.position.isDefined option frag(" • ", trans.thematic()),
          " • ",
          t.mode.fold(trans.casualTournament, trans.ratedTournament)(),
          " • ",
          t.durationString
        )
      )
    )
}
