package views.html
package userTournament

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator

object created:

  private val path = "created"

  def apply(u: User, pager: Paginator[lila.tournament.Tournament])(using PageContext) =
    bits.layout(
      u = u,
      title = s"${u.username} created tournaments",
      path = path,
      modules = infiniteScrollTag
    ):
      if pager.nbResults == 0 then div(cls := "box-pad")(trans.site.nothingToSeeHere())
      else
        div(cls := "tournament-list")(
          table(cls := "slist")(
            thead(
              tr(
                th(cls := "count")(pager.nbResults),
                th(colspan := 2)(h1(frag(userLink(u, withOnline = true), " • ", trans.site.tournaments()))),
                th(trans.site.winner()),
                th(trans.site.players())
              )
            ),
            tbody(cls := "infinite-scroll")(
              pager.currentPageResults.map { t =>
                tr(cls := "paginated")(
                  td(cls := "icon")(iconTag(tournamentIcon(t))),
                  views.html.tournament.finishedList.header(t),
                  td(momentFromNow(t.startsAt)),
                  td(cls := "winner")(
                    t.winnerId.isDefined.option(userIdLink(t.winnerId, withOnline = false))
                  ),
                  td(cls := "text", dataIcon := Icon.User)(t.nbPlayers.localize)
                )
              },
              pagerNextTable(pager, np => routes.UserTournament.path(u.username, path, np).url)
            )
          )
        )
