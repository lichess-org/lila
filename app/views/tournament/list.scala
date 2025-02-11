package views.html.tournament

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.tournament.Tournament

object list {

  def paginate(pager: Paginator[Tournament], url: String)(implicit
      ctx: Context,
  ) =
    if (pager.currentPageResults.isEmpty)
      div(cls := "notours")(
        iconTag("4"),
        p(trans.study.noneYet()),
      )
    else
      table(cls := "slist")(
        thead(
          tr(
            th(colspan := 2)(),
            th(trans.search.date()),
            th(trans.players()),
          ),
        ),
        tbody(cls := "tours list infinitescroll")(
          pagerNextTable(pager, np => addQueryParameter(url, "page", np)),
          pager.currentPageResults.map { t =>
            tr(cls := "paginated")(
              td(cls := "icon")(
                i(cls := t.format.key, dataIcon := tournamentIconChar(t)),
              ),
              header(t),
              td(cls := "date")(momentFromNow(t.startsAt)),
              td(cls := "players")(
                span(
                  t.winnerId.isDefined option frag(
                    i(cls := "text winner", dataIcon := "g"),
                    userIdLink(t.winnerId, withOnline = false),
                  ),
                ),
                span(
                  i(cls := "text nb-players", dataIcon := "r"),
                  trans.nbPlayers.pluralSameTxt(t.nbPlayers),
                ),
              ),
            )
          },
        ),
      )

  def header(t: Tournament)(implicit ctx: Context) =
    td(cls := "header")(
      a(href := routes.Tournament.show(t.id))(
        span(cls := "name")(tournamentName(t)),
        span(cls := "setup")(
          t.timeControl.show,
          " - ",
          t.perfType.trans,
          t.position.isDefined option frag(" - ", trans.thematic()),
          " - ",
          t.mode.fold(trans.casualTournament, trans.ratedTournament)(),
          " - ",
          t.format.trans,
        ),
      ),
    )
}
