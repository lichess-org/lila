package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.Tournament

import controllers.routes

object finishedList {

  def apply(finished: List[Tournament])(implicit ctx: Context) =
    tbody(
      finished.map { t =>
        tr(cls := List("tour-scheduled" -> t.isScheduled))(
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

  def header(t: Tournament)(implicit ctx: Context) =
    td(cls := "header")(
      a(href := routes.Tournament.show(t.id))(
        span(cls := "name")(t.name()),
        span(cls := "setup")(
          t.clock.show,
          " - ",
          if (!t.variant.standard) variantName(t.variant) else t.perfType.map(_.trans),
          t.position.isDefined option frag(" - ", trans.thematic()),
          " - ",
          t.mode.fold(trans.casualTournament, trans.ratedTournament)(),
          " - ",
          t.durationString
        )
      )
    )
}
