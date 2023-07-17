package views.html.tournament

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.tournament.Tournament

import controllers.routes

object finishedList:

  def apply(finished: List[Tournament])(using PageContext): Tag =
    tbody(finished map apply)

  def apply(t: Tournament)(using PageContext): Tag =
    tr(cls := "paginated")(
      td(cls := "icon")(iconTag(tournamentIcon(t))),
      header(t),
      td(cls := "date")(momentFromNow(t.startsAt)),
      td(cls := "players")(
        span(
          iconTag(licon.Trophy)(cls := "text"),
          userIdLink(t.winnerId, withOnline = false)
        ),
        span(trans.nbPlayers.plural(t.nbPlayers, t.nbPlayers.localize))
      )
    )

  def header(t: Tournament)(using PageContext) =
    td(cls := "header")(
      a(href := routes.Tournament.show(t.id))(
        span(cls := "name")(t.name()),
        span(
          t.clock.show,
          " • ",
          if t.variant.exotic then t.variant.name else t.perfType.trans,
          t.position.isDefined option frag(" • ", trans.thematic()),
          " • ",
          if t.mode.rated then trans.ratedTournament() else trans.casualTournament(),
          " • ",
          t.durationString
        )
      )
    )
