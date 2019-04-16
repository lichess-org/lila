package views.html
package userTournament

import lidraughts.api.Context
import lidraughts.user.User
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator

import controllers.routes

object list {

  def apply(u: User, path: String, pager: Paginator[lidraughts.tournament.LeaderboardApi.TourEntry], count: String)(implicit ctx: Context) =
    if (pager.nbResults == 0) {
      div(cls := "box-pad")(u.username, " hasn't played in any tournament yet!")
    } else {
      div(cls := "tournament-list")(
        table(cls := "slist")(
          thead(
            tr(
              th(cls := "count")(count),
              th(h1(userLink(u, withOnline = true), " tournaments")),
              th("Games"),
              th("Points"),
              th("Rank")
            )
          ),
          tbody(cls := "infinitescroll")(
            pager.nextPage.map { np =>
              tr(
                th(cls := "pager none")(
                  a(rel := "next", href := routes.UserTournament.path(u.username, path, np))("Next")
                )
              )
            },
            pager.currentPageResults.map { e =>
              tr(cls := List("paginated" -> true, "scheduled" -> e.tour.isScheduled))(
                td(cls := "icon")(iconTag(tournamentIconChar(e.tour))),
                td(cls := "header")(
                  a(href := routes.Tournament.show(e.tour.id))(
                    span(cls := "name")(e.tour.fullName),
                    span(cls := "setup")(
                      e.tour.clock.show, " • ",
                      if (e.tour.variant.exotic) e.tour.variant.name else e.tour.perfType.map(_.name), " • ",
                      momentFromNow(e.tour.startsAt)
                    )
                  )
                ),
                td(cls := "games")(e.entry.nbGames),
                td(cls := "score")(e.entry.score),
                td(cls := "rank")(strong(e.entry.rank), " / ", e.tour.nbPlayers)
              )
            }
          )
        )
      )
    }
}
